# Orçamentos Service

Gerencia **clientes, orçamentos, itens e o pipeline (kanban)** do produto EHS Orçamentos.

---

## Responsabilidades
- CRUD de clientes (com observações)
- CRUD de orçamentos com itens, validade, termos e observações
- Transições de status (rascunho, aprovação, execução, conclusão)
- Dados do dashboard e colunas do kanban
- Isolamento por usuário via `X-User-ID` (enviado pelo gateway)

## Endpoints
- `GET/POST /clients`, `GET/PUT/DELETE /clients/{id}`
- `GET/POST /budgets`, `GET/PUT/DELETE /budgets/{id}`
- `GET /dashboard`, `GET /dashboard/kanban`
- `GET /ping`

## Banco
PostgreSQL, database `budgets`.
