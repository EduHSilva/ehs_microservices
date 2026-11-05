# Finances API: contas, metas, importação e projeção

Todas as rotas abaixo ficam sob `/finances` no gateway e usam o usuário autenticado do `UserContext`. O cliente não envia `userId`. Consultas e gravações existentes permanecem isoladas pelo filtro Hibernate por usuário; operações vinculadas a contas verificam o proprietário autenticado.

## Identificador de conta

`GET /finances/accounts` retorna somente contas do usuário autenticado e cria uma conta PF padrão com saldo inicial zero se ainda não existir. Na primeira criação, transações/recorrências antigas sem `accountId` são associadas a essa PF; transações legadas vinculadas a uma recorrência herdam sua direção de entrada/saída. `POST /finances/accounts` cria conta com `name`, `type` (`PF`/`PJ`) e `openingBalance`; respostas incluem o `id` estável e o saldo atual. A moeda inicial é BRL. Transações e recorrências aceitam `accountId` (string de até 80 caracteres) e validam que a conta pertence ao usuário; omitir o campo em clientes antigos associa novos registros à conta PF padrão. Listagens aceitam `?accountId=...` e validam a titularidade quando informado. Metas e importação exigem `accountId`.

Não há compartilhamento de conta entre usuários neste rollout. O proprietário é o único usuário autorizado; um `accountId` de outra pessoa resulta em 404. As preferências de exibição PJ não criam contas nem concedem acesso. A conta PJ só aparece para quem a possui. Para compartilhamento empresarial futuro, será necessário integrar permissões de membros ao filtro de dados e às categorias do domínio.

## Metas mensais

- `GET /finances/goals?accountId={id}&periodMonth=YYYY-MM&page=0&size=50` (size máximo 200)
- `POST /finances/goals`
- `PUT /finances/goals/{id}`
- `DELETE /finances/goals/{id}`
- `GET /finances/goals/{id}/summary?periodMonth=YYYY-MM`
- `POST /finances/goals/{id}/contributions`
- `DELETE /finances/goal-contributions/{contributionId}`

O corpo da meta contém `accountId`, `title`, `mode` (`FIXED` ou `PERCENT_INCOME`), `amount`, `periodMonth` e `active`. Percentuais aceitam de 0,01 a 100. A base percentual soma apenas entradas de transações com status `OK` e mesma conta no mês, com competência por `executionDate` em UTC. Aporte é uma realização própria e pode opcionalmente apontar para uma transação da mesma conta; cada transação só pode ser vinculada uma vez. Alterar uma meta para um mês atualiza a apuração daquele mês inteiro (não há versionamento por dia).

## Importação de extratos

- `POST /finances/transactions/import/preview`
- `POST /finances/transactions/import/confirm`
- `PUT /finances/transactions/{id}/review`

Prévia e confirmação recebem `accountId`, `source` e até 1.000 linhas já interpretadas pelo cliente. Cada linha contém `description`, `amount` positivo, `income`, `date` (`YYYY-MM-DD`), `categoryId` opcional e `sourceTransactionId` opcional. O servidor valida os campos, cria uma chave SHA-256 por conta e referência (ou, sem referência, por data/valor/direção/descrição normalizada), e retorna duplicatas na prévia. A confirmação ignora chaves repetidas e salva as novas transações com status `REVIEW`, preservando descrição original e categoria opcional. A revisão recebe `{ "approved": true, "categoryId": "..." }` para validar e efetivar; com `approved: false`, descarta o lançamento.

O arquivo original não é armazenado e o serviço não registra conteúdo das linhas em logs. A chave de fallback pode colidir em duas operações legítimas com mesma data, valor, direção e descrição; bancos que fornecem identificador estável devem enviá-lo em `sourceTransactionId`. O endpoint recebe linhas já interpretadas pelo web; não recebe multipart nem converte CSV/OFX no servidor. `GET /finances/transactions/{id}/import-audit` lista eventos sem gravar valores ou descrições no log de auditoria.

## Projeção de fluxo de caixa

`GET /finances/dashboard/projection?accountId={id}&days=30|90|180` retorna série diária, saldo projetado, itens confirmados/recorrentes, estimativas variáveis e nível de confiança. O saldo base usa saldo de abertura e transações `OK` até hoje. Recorrências mensais e parcelas ativas são previstas pelo dia cadastrado; transações futuras vinculadas à recorrência substituem a ocorrência daquele mês. Entradas e saídas variáveis usam médias dos seis meses completos anteriores, excluindo lançamentos já associados a recorrências, distribuídas pelos dias do mês. A confiança é `LOW` (<6 transações históricas variáveis), `MEDIUM` (6–23) ou `HIGH` (24+). A série é uma estimativa, não altera o saldo realizado. O horizonte aceita apenas 30, 90 ou 180 dias.
