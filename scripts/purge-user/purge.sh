#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

usage() {
    cat <<'USAGE'
Uso: purge.sh --user <uuid> --email <email> --scope <escopo> (--dry-run | --yes) [opções]

Escopos:
  all       módulos + notificações + conta (users por último)
  budgets   somente budgets
  finances  somente finances (aborta se o titular tem conta com membro ATIVO)
  study     study + tasks
  fitness   fitness (banco health)

Opções:
  --dry-run               executa cada função e faz ROLLBACK
  --yes                   confirma a execução real (obrigatório sem --dry-run)
  --backup-dir <dir>      raiz dos backups (default: /var/backups/ehs-lgpd)
  --log <arquivo>         log de auditoria (default: /var/log/ehs/purge-user.log)
  --compose-file <arq>    default: docker-compose.prod.yml
  --postgres-service <s>  default: postgres
  --help                  mostra esta ajuda

A solicitação é somente por e-mail; este script não é exposto pelo painel.
USAGE
}

USER_ID=""
EMAIL=""
SCOPE=""
MODE=""
BACKUP_ROOT="/var/backups/ehs-lgpd"
LOG_FILE="/var/log/ehs/purge-user.log"
COMPOSE_FILE="docker-compose.prod.yml"
PG_SERVICE="postgres"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --user) USER_ID="${2:-}"; shift 2 ;;
        --email) EMAIL="${2:-}"; shift 2 ;;
        --scope) SCOPE="${2:-}"; shift 2 ;;
        --dry-run) MODE="dry-run"; shift ;;
        --yes) MODE="execute"; shift ;;
        --backup-dir) BACKUP_ROOT="${2:-}"; shift 2 ;;
        --log) LOG_FILE="${2:-}"; shift 2 ;;
        --compose-file) COMPOSE_FILE="${2:-}"; shift 2 ;;
        --postgres-service) PG_SERVICE="${2:-}"; shift 2 ;;
        --help|-h) usage; exit 0 ;;
        *) echo "Opção desconhecida: $1" >&2; usage >&2; exit 2 ;;
    esac
done

if [[ -z "$USER_ID" || -z "$EMAIL" || -z "$SCOPE" ]]; then
    echo "Informe --user, --email e --scope." >&2
    usage >&2
    exit 2
fi
if [[ -z "$MODE" ]]; then
    echo "Use --dry-run para simular ou --yes para executar." >&2
    exit 2
fi
if [[ ! "$USER_ID" =~ ^[0-9a-fA-F-]{36}$ ]]; then
    echo "UUID inválido: $USER_ID" >&2
    exit 2
fi

case "$SCOPE" in
    all) DBS=(finances budgets study tasks health notifications users) ;;
    budgets) DBS=(budgets) ;;
    finances) DBS=(finances) ;;
    study) DBS=(study tasks) ;;
    fitness) DBS=(health) ;;
    *) echo "Escopo inválido: $SCOPE" >&2; usage >&2; exit 2 ;;
esac

if [[ -z "${POSTGRES_USER:-}" && -f .env ]]; then
    POSTGRES_USER="$(sed -n 's/^POSTGRES_USER=//p' .env | tail -n1 | tr -d '\r' | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")"
fi
PG_USER="${POSTGRES_USER:-postgres}"

if ! docker compose -f "$COMPOSE_FILE" ps --status running --services | grep -qx "$PG_SERVICE"; then
    echo "Serviço $PG_SERVICE não está rodando no compose $COMPOSE_FILE." >&2
    exit 1
fi

mkdir -p "$(dirname "$LOG_FILE")"
log() {
    printf '%s %s\n' "$(date -u +%FT%TZ)" "$*" >> "$LOG_FILE"
}

PSQL_SCALAR=(docker compose -f "$COMPOSE_FILE" exec -T "$PG_SERVICE" \
    psql -U "$PG_USER" -X -q -t -A -v ON_ERROR_STOP=1)
PSQL_RUN=(docker compose -f "$COMPOSE_FILE" exec -T "$PG_SERVICE" \
    psql -U "$PG_USER" -X -v ON_ERROR_STOP=1 -P pager=off)

for db in "${DBS[@]}"; do
    exists="$("${PSQL_SCALAR[@]}" -d "$db" \
        -c "SELECT to_regprocedure('lgpd_purge_user(uuid,text)') IS NOT NULL")"
    if [[ "$exists" != "t" ]]; then
        echo "Função lgpd_purge_user ausente em $db. Faça deploy da versão atual do serviço antes de purgar." >&2
        exit 1
    fi
done

stored_email="$("${PSQL_SCALAR[@]}" -d users \
    -c "SELECT email FROM users WHERE id = '$USER_ID'")"
if [[ -z "$stored_email" ]]; then
    echo "Usuário $USER_ID não encontrado na base users." >&2
    exit 1
fi
if [[ "${stored_email,,}" != "${EMAIL,,}" ]]; then
    echo "AVISO: --email ($EMAIL) difere do e-mail cadastrado ($stored_email)." >&2
fi

run_function() {
    local db="$1"
    local tx_end="$2"
    "${PSQL_RUN[@]}" -d "$db" -v target="$USER_ID" -v email="$EMAIL" <<SQL
BEGIN;
SELECT * FROM lgpd_purge_user(:'target'::uuid, :'email');
$tx_end;
SQL
}

log "INICIO mode=$MODE scope=$SCOPE user=$USER_ID email=$EMAIL dbs=${DBS[*]}"

echo "== Preflight (transação com rollback) =="
for db in "${DBS[@]}"; do
    echo "-- $db"
    if ! run_function "$db" "ROLLBACK"; then
        log "FALHA preflight db=$db"
        echo "Preflight falhou em $db. Nada foi gravado." >&2
        exit 1
    fi
done

if [[ "$MODE" == "dry-run" ]]; then
    log "OK dry-run scope=$SCOPE user=$USER_ID"
    echo "Dry-run concluído. Nada foi gravado."
    exit 0
fi

STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
BACKUP_DIR="$BACKUP_ROOT/$STAMP"
mkdir -p "$BACKUP_DIR"
echo "== Backup em $BACKUP_DIR =="
for db in "${DBS[@]}"; do
    echo "-- $db"
    if ! docker compose -f "$COMPOSE_FILE" exec -T "$PG_SERVICE" \
        pg_dump -U "$PG_USER" -Fc -d "$db" > "$BACKUP_DIR/$db.dump"; then
        log "FALHA backup db=$db dir=$BACKUP_DIR"
        echo "Backup de $db falhou." >&2
        exit 1
    fi
    if [[ "$(head -c 5 "$BACKUP_DIR/$db.dump" 2>/dev/null || true)" != "PGDMP" ]]; then
        log "FALHA backup-invalido db=$db dir=$BACKUP_DIR"
        echo "Backup de $db inválido." >&2
        exit 1
    fi
done

echo "== Execução =="
for db in "${DBS[@]}"; do
    echo "-- $db"
    if ! run_function "$db" "COMMIT"; then
        log "FALHA execucao db=$db backup=$BACKUP_DIR"
        echo "Falha em $db. Os bancos anteriores já foram gravados; use o backup $BACKUP_DIR se precisar restaurar." >&2
        exit 1
    fi
done

log "OK mode=execute scope=$SCOPE user=$USER_ID backup=$BACKUP_DIR"
echo "Purga concluída. Backup: $BACKUP_DIR"
