# Regras de negócio — plataforma EHS (microservices)

> Vale para todos os serviços. Regras específicas de cada produto ficam no
> `regras-negocio.md` do repositório correspondente.

## 1. Contas e autenticação

- Toda conta pertence a **um** usuário (`users`). Não há conta compartilhada.
- Uma conta tem status (`UserStatus`): `OK`, `WAITING_CONFIRMATION`, `DELETED`, `BLOCKED`.
  - O cadastro sempre cria `WAITING_CONFIRMATION`; a conta só entra depois da confirmação.
  - `WAITING_CONFIRMATION → OK` pela confirmação; `OK ⇄ BLOCKED` só por ADMIN;
    `→ DELETED` só por exclusão administrativa (soft delete) e não há retorno.
  - Bloquear incrementa `tokenVersion`, derrubando as sessões abertas.
- O login exige o campo `app` (`ADMIN`, `BUDGETS`, `FINANCES`, `STUDY`, `FITNESS`).
  O usuário precisa ter ao menos uma role do app; senão 403. Erros:
  - **401** = não autenticado (token ausente/inválido, sessão expirada) → os fronts
    limpam a sessão e voltam ao login.
  - **403** = autenticado sem permissão (app/role/ADMIN) → os fronts apenas avisam
    "sem permissão", **sem** deslogar.
- Senhas são BCrypt; reset por e-mail é token de uso único (30 min) e qualquer mudança
  de `tokenVersion` invalida os tokens pendentes.
- Login com Google exige **cadastro prévio** (e-mail precisa existir e estar `OK`) e o
  claim `email_verified = true`; o Google serve como segundo fator de identidade, não
  como provisionamento de conta.
- `tokenVersion` (`ver` no JWT) é incrementado ao trocar senha, alterar roles,
  bloquear, confirmar acesso a app e "sair de todas as sessões".
- Segurança dos dados: o e-mail é normalizado (trim + lowercase) e único; senhas nunca
  aparecem em DTOs/logs.

## 2. Roles e escopo de aplicação

| App (`AppScope`) | Roles aceitas |
| --- | --- |
| `ADMIN` | `ADMIN` |
| `BUDGETS` | `BUDGETS` |
| `FINANCES` | `FINANCES` |
| `STUDY` | `STUDY` |
| `FITNESS` | `HEALTH_USER`, `HEALTH_PERSONAL`, `HEALTH_NUTRITIONIST` |

- `ADMIN` passa em todos os apps (acesso administrativo).
- Role `ADMIN` só existe via seed; não pode ser concedida nem removida pela API.
- Roles de ADMIN são geridas apenas por ADMIN; o próprio usuário não altera as suas.
- O gateway valida token + `aud` (app) + role da rota e injeta `X-User-ID`; o serviço
  de destino confia nesse header.

## 3. Acesso a várias aplicações

- Quem já tem conta e se cadastra em outro app recebe um link de **vínculo de acesso**
  (`APP_ACCESS`) que adiciona a role **sem alterar a senha**.
- Contas `BLOCKED`/`DELETED` não recebem e-mail e não geram novo cadastro.
- A confirmação do vínculo não passa por ADMIN (é feita pelo dono do e-mail).
- As páginas `/user/confirm/{id}` e `/user/app-access` são estáticas (mesmo padrão do
  reset) e chamam a API; `GET /users/confirm/{id}` é público no gateway e não devolve
  dados do usuário.

## 4. Empresas

- Cada usuário pode ter **uma** empresa; ela é criada no cadastro apenas para
  `BUDGETS`, `HEALTH_PERSONAL` e `HEALTH_NUTRITIONIST` (nome derivado do usuário).
- A empresa pode ser renomeada pelo dono (`PUT /companies/{id}`).
- Excluir a empresa exclui (soft delete) todos os usuários dela.
- Listagem/consulta/exportação de empresas é **ADMIN-only** (`@OnlyAdmin` no service,
  rota sem acesso para usuário comum; validado: 403 para role não-ADMIN).

## 5. Isolamento de dados (multi-tenant)

- Cada linha de domínio pertence a um `user_id`; nenhum usuário vê dado de outro.
- No serviço `budgets` o isolamento é feito por filtro Hibernate (`userFilter`) + queries
  `findByIdAndUserId`; no `finances` as entidades têm `owner_id`/`user_id` nas queries;
  em `fitness`/`study`/`tasks` as queries são sempre por usuário do token.
- `@Filter` **não** vale para `findById`: leituras por id sempre usam variante por usuário.
- Bulk updates precisam repetir `AND user_id = :userId`.
- Requisição sem `X-User-ID` em rota privada = 401 (falha fechada).
- O `budgets` não tem Spring Security: ele confia no `X-User-ID`, então a porta do
  serviço não pode ser publicada no host (só via gateway).

## 6. Exclusão de dados

- Exclusões de negócio são **soft delete** (`status = DELETED` + `@SQLRestriction`).
- Exclusão via `DELETE`; o `PUT` rejeita `DELETED` com 400 em todas as entidades com status.
- Registros excluídos continuam no banco para histórico/auditoria.

## 7. Links e páginas centrais

- Domínio único servido pelo nginx: landing em `/app/`, política de privacidade em
  `/app/privacidade/`, reset de senha em `/user/reset-password`.
- E-mails transacionais usam `SERVER_URL` como base; em produção precisa ser o domínio
  público (HTTPS).
- A política de privacidade é única para todos os produtos e cobre LGPD/Loja.

## 8. Observabilidade e auditoria

- Auditoria de operações administrativas (create/update/delete/status/confirmação/
  reenvio/invalidação de sessão) é registrada em log, sem tabela própria.
- Dados sensíveis (senha, tokens, valores financeiros) não entram em log nem em OpenAPI.
