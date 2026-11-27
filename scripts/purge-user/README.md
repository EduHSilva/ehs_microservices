# Runbook — purga manual de dados de usuário (LGPD)

> **Status:** implementado. A função `lgpd_purge_user` é criada no boot de cada serviço e o
> script `scripts/purge-user/purge.sh` orquestra dry-run, backup e execução. A solicitação é
> **somente por e-mail**; não existe botão no painel. Não executar sem ticket, identidade
> verificada e aprovação.

## 1. Como funciona

- Cada banco de serviço tem uma função PL/pgSQL `lgpd_purge_user(p_user_id uuid, p_email text)`.
- O SQL fica versionado no próprio serviço (`src/main/resources/db/lgpd_purge.sql` no Java,
  `fitness/config/lgpd_purge.sql` no Go) e é (re)aplicado a cada boot por um installer
  (`@Profile("!test")` + checagem de produto `PostgreSQL`; no Go, via `db.Exec`).
- A função é **idempotente**: rodar de novo após a janela de sessão não encontra mais linhas.
- O retorno é `TABLE(tabela text, excluidos bigint)`; o script usa isso para o dry-run.

Chamada sempre dentro de transação (a contagem usa tabela temporária com `ON COMMIT DROP`):

```sql
BEGIN;
SELECT * FROM lgpd_purge_user(:'target'::uuid, :'email');
COMMIT;  -- ou ROLLBACK no dry-run
```

## 2. Escopos, bancos e ordem

| Escopo | Bancos |
| --- | --- |
| `all` | `finances` → `budgets` → `study` → `tasks` → `health` → `notifications` → `users` |
| `budgets` | `budgets` |
| `finances` | `finances` |
| `study` | `study` + `tasks` (não há task sem study) |
| `fitness` | `health` |

`users` sempre por último quando incluído. `payments` não armazena dados de usuário hoje.

Ordem interna (respeita as FKs):

| Banco | Tabelas |
| --- | --- |
| `budgets` | `item` → `budgets` → `clients` |
| `finances` | `transaction_import_audit` → `goal_contribution` → `"transaction"` → `recurrences` → `credit_card` → `monthly_goal` → `category` → `finance_account_member` → `finance_account` → `user_preference` |
| `study` | `notes` → `project_stages` → `project_versions` → `projects` → `folders` → `focus_sessions` |
| `tasks` | `task_completions` → `tasks` |
| `health` | `exercise_workouts` → `meal_foods` → `workouts` → `meals` → `user_configs` → `histories` → `student_professionals` |
| `notifications` | `email` (pela coluna do dono descoberta no schema) |
| `users` | `companies`/`users.company_id` (tratamento especial) → tabela de roles → `users` |

Regras de escopo por tabela:

- budgets: orçamentos/itens do usuário e também os de clientes dele; `clients` só por `user_id`.
- finances: transações/recorrências/cartões/metas da conta própria **ou** do usuário; categorias
  só por `user_id`; membros por `user_id`, e-mail ou conta própria.
- health: treinos/refeições do usuário como aluno **ou** autor; vínculos
  `student_professionals` por aluno, profissional ou `invited_email`.
- **Catálogos globais do fitness (`foods`, `exercises`) nunca são apagados.**
- Soft delete não protege: o `DELETE` bruto apaga fisicamente, inclusive linhas já soft-deleted
  (`@SQLDelete`/`deleted_at`). É o desejado aqui.

## 3. Travas e cenários especiais

**Drift check (adaptação a novas tabelas/colunas).** Antes de apagar, a função procura no
schema `public` colunas com cara de dono (`user_id`, `userid`, `owner_id`, `student_id`,
`author_id`, `professional_id`, `account_id`, `invited_email`, variações) fora da lista
coberta, e FKs de tabelas não cobertas apontando para tabelas cobertas. Se encontrar, aborta:

```
lgpd_purge_user: revisar cobertura do schema (colunas de dono: x; filhas de tabela coberta: y)
```

Nada é apagado. Ajuste a função (ver §9), faça deploy do serviço e rode de novo.

**Finances — conta compartilhada.** Se o titular possui conta com membro `ACTIVE` que não é
ele mesmo (por `user_id`/e-mail), a função aborta e lista as contas. Revogue ou transfira os
membros antes de repetir. Membros do próprio titular não bloqueiam.

**Users — empresa.** Para cada empresa do titular:

- empresa `DELETED`: `owner_id` é anulado;
- empresa ativa com outro usuário elegível (não `DELETED`, que ainda não é dono de outra
  empresa): titularidade transferida para o mais antigo; o `company_id` do titular é anulado;
- empresa ativa com outros usuários, mas nenhum elegível: **aborta** (resolver manualmente);
- empresa sem outros usuários ativos: `company_id` de todos é anulado e a empresa é apagada.

Usuário com role `ADMIN` **aborta**: remova a role antes.

**Sessão.** Só o serviço `users` valida status/`token_version`; JWT emitido continua aceito
nos serviços de produto por até **2 h**, e alguns GETs recriam defaults. Recomendado:
bloquear/excluir a conta antes, purgar, e repetir a purga ao fim da janela de 2 h.

**Notificações.** Só linhas com o dono (`userid`) são apagadas. Uma mensagem endereçada ao
e-mail do titular, mas pertencente a outro usuário, permanece.

## 4. Pré-requisitos

- Stack no ar com o serviço `postgres` do compose.
- Deploy já feito com a versão atual dos serviços (instala/atualiza a função no boot).
  Verificação:
  `SELECT to_regprocedure('lgpd_purge_user(uuid,text)') IS NOT NULL;` em cada banco.
- `POSTGRES_USER` no `.env` do diretório do compose (lido só essa chave; o script não faz
  `source` do arquivo).
- Acesso ao host do compose (em produção: `/opt/stacks/microservices`).

Logs de boot confirmam a instalação nos serviços Java:

```
INFO ... LgpdPurgeInstaller : Função lgpd_purge_user disponível neste banco
```

No fitness a instalação é silenciosa; se falhar, o serviço não sobe.

## 5. Uso do script

```bash
scripts/purge-user/purge.sh --user <uuid> --email <email> --scope <escopo> --dry-run
scripts/purge-user/purge.sh --user <uuid> --email <email> --scope <escopo> --yes
```

Opções:

| Opção | Default | Função |
| --- | --- | --- |
| `--scope` | — | `all`, `budgets`, `finances`, `study`, `fitness` |
| `--dry-run` | — | preflight com `ROLLBACK` em todos os bancos |
| `--yes` | — | executa de verdade (preflight + backup + `COMMIT`) |
| `--backup-dir` | `/var/backups/ehs-lgpd` | raiz dos dumps (subpasta por timestamp) |
| `--log` | `/var/log/ehs/purge-user.log` | auditoria (sem senha/dados) |
| `--compose-file` | `docker-compose.prod.yml` | use `docker-compose.yml` em dev |
| `--postgres-service` | `postgres` | nome do serviço no compose |

Comportamento:

1. valida função presente em todos os bancos do escopo;
2. confere o e-mail contra `users.email` (avisa se divergir, não bloqueia);
3. dry-run de cada banco com `ROLLBACK`;
4. em execução: `pg_dump -Fc` de cada banco em `<backup-dir>/<timestamp>/` (valida header
   `PGDMP`) e só então executa `BEGIN → função → COMMIT` banco a banco;
5. grava linhas `INICIO`/`OK`/`FALHA` no log.

Se um banco falhar no meio da execução, os anteriores já foram gravados: corrija e repita
(idempotente) ou restaure do backup (§7).

Exemplo local (teste):

```bash
bash scripts/purge-user/purge.sh \
  --user a0000000-0000-4000-8000-000000000001 \
  --email alguem@example.com \
  --scope all --dry-run \
  --compose-file docker-compose.yml \
  --backup-dir ./.tmp/lgpd-backup \
  --log ./.tmp/purge-user.log
```

## 6. Fluxo operacional

1. **Intake:** solicitação por e-mail; registre no ticket e-mail do solicitante (deve ser o
   cadastrado), escopo, UUID, `company_id`, roles, data/hora e prazo de resposta.
2. **Congelar:** bloqueie a conta pelo painel/API antes da purga (reduz a janela de escrita).
3. **Dry-run:** `--dry-run` e revise as contagens; zero onde deveria haver dados indica UUID
   errado.
4. **Executar:** `--yes` (gera backup antes).
5. **Repetir:** após ≥ 2 h, dry-run de novo; se houver linhas recriadas, execute outra vez.
6. **Validar:** contagens zeradas, login do usuário falha, nenhuma linha nova reaparece.
7. **Backup:** contém dados pessoais — acesso restrito, retenção curta (ex.: 30 dias) e
   destruição depois.

## 7. Restauração (incidente)

Não restaure por cima do banco em produção com os serviços no ar. Para recuperação pontual:

```bash
docker compose -f docker-compose.prod.yml exec -T postgres \
  createdb -U "${POSTGRES_USER:-postgres}" users_restore
docker compose -f docker-compose.prod.yml exec -T postgres \
  pg_restore -U "${POSTGRES_USER:-postgres}" -d users_restore < "<backup-dir>/users.dump"
# extraia as linhas necessárias e reinsira; depois:
docker compose -f docker-compose.prod.yml exec -T postgres \
  dropdb -U "${POSTGRES_USER:-postgres}" users_restore
```

Restaurar reintroduz dados pessoais já apagados: use só para corrigir erro de execução, com
aprovação, e destrua a cópia depois.

## 8. Auditoria

O script grava em log restrito (UUID e e-mail de referência, sem senha nem valores
financeiros):

```
2026-11-24T20:48:02Z INICIO mode=execute scope=all user=<uuid> email=<email> dbs=finances budgets ...
2026-11-24T20:48:12Z OK mode=execute scope=all user=<uuid> backup=<dir>
2026-11-24T20:48:41Z FALHA preflight db=finances
```

Complemente no ticket com operador, ticket e resultado. Se for criada uma tabela
`purge_audit`, grave os mesmos campos nela; hoje a auditoria é log + dumps.

## 9. Manutenção do procedimento

Ao adicionar tabela/coluna de dono em um serviço:

1. O build já falha: cada serviço tem `LgpdPurgeCoverageTest` (Java/Kotlin) e o fitness tem
   `config/lgpd_purge_test.go`, que comparam o schema do código com o `v_covered` do SQL.
2. No SQL do serviço, inclua a tabela em `v_covered` e adicione o `DELETE` na ordem correta
   (filhas antes das mães; use subselects por conta/projeto/tarefa quando a coluna não for
   `user_id`).
3. Catálogos globais (`foods`, `exercises`) entram em `v_covered` só para o drift check
   ignorá-los; nunca recebem `DELETE` (o teste do Go garante isso).
4. Faça deploy do serviço: o installer aplica `CREATE OR REPLACE` no boot.
5. Rode `--dry-run` com um usuário que tenha dados na tabela nova antes de confiar no fluxo.

Detalhes dos testes de build:

- Java: o teste sobe o contexto com o perfil `test` (H2, `ddl-auto=create-drop`), lê
  `v_covered` e a lista de colunas de dono direto do SQL e confere tabelas, colunas de dono
  e FKs filhas. `user_roles` é declarada à parte porque a função a descobre via FK.
- Go: o teste usa `domainModels` (a mesma lista do `AutoMigrate` em `db.go`), deriva os nomes
  de tabela pelo naming do GORM e exige cobertura de todas as tabelas de domínio.
- Roda no CI (`mvn clean verify` e `go test ./...`). Em build incremental local, classes de
  uma versão anterior em `target/classes` podem gerar falso positivo (tabela que não existe
  mais no código); nesse caso rode `mvn clean test`.
- A falha indica exatamente o nome da tabela/coluna; corrigir o SQL e o `v_covered` faz o
  teste voltar a passar. O drift check em runtime continua como trava final no banco real.

## 10. Decisões pendentes (ver `TODO.md`)

- Retenção legal de dados financeiros/fiscais vs. "deletar tudo" (por ora: apaga tudo).
- E-mail da conta: o hard delete libera o e-mail para novo cadastro.
- Revogação durável de sessão (hoje expira em 2 h; avaliação de tombstone no gateway).
- `author_id` do profissional no fitness permanece como UUID pseudônimo após a purga.
- Exceção à regra §6 de `regras-negocio.md`: estes fluxos são hard delete; o resto continua
  soft delete.
- Migrações versionadas para a função (por ora, boot + `CREATE OR REPLACE`).
