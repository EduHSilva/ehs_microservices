# Regras de negócio — plataforma EHS (microservices)

> Vale para todos os serviços. Regras específicas de cada produto ficam no
> `regras-negocio.md` do repositório correspondente.

## 1. Contas e autenticação

- Toda conta de login pertence a **um** usuário (`users`). Compartilhamento de dados
  entre usuários existe apenas no `finances` (conta financeira compartilhada por
  convite); as regras ficam no `regras-negocio.md` do produto.
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
- Validade da sessão: o JWT vale `API_SECURITY_TOKEN_TTL_HOURS` (padrão 168 h = 7 dias)
  e **`GET /auth/me` devolve um token renovado** (expiração deslizante). Os clientes
  devem persistir o token devolvido por `/auth/me`, de modo que quem usa o app com
  frequência não precise entrar de novo; inatividade maior que a validade força novo
  login (401). O cookie dos sites (Finanças e Orçamentos) é persistente (30 dias) —
  a validade real continua sendo a do JWT.
- Troca de senha autenticada (`PUT /auth/password`): exige a senha atual correta, nova
  senha com ao menos 8 caracteres e diferente da atual. Senha atual errada retorna 400
  (não 401) para os fronts não derrubarem a sessão. A troca persiste só o hash e
  incrementa `tokenVersion`, encerrando as demais sessões; a sessão que fez a troca
  recebe um token novo na resposta (mesmo `aud` do token usado na chamada).
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
- O gateway valida token + `aud` (app) + role da rota e injeta `X-User-ID` e
  `X-User-Roles` (CSV); o serviço de destino confia nesses headers.
- Rotas `/internal/**` são bloqueadas no gateway com 404, mesmo com token válido. A
  consulta interna do `fitness` ao `users` (`/internal/profiles`) usa o segredo
  compartilhado `X-Internal-Token` e nunca é exposta ao cliente.
- No `fitness`, o gateway garante apenas `aud` + role `HEALTH_*`; o serviço valida o
  vínculo aluno ↔ profissional, o domínio (treinos vs dieta) e o modo somente leitura do
  aluno.
- Exceção intencional: as rotas do serviço `users` **não** são restritas por role no
  gateway, porque ele é compartilhado por todos os produtos (auto-perfil e vínculo de
  acesso). A proteção administrativa fica no próprio serviço
  (`@OnlyAdmin`/`@PreAuthorize`), que devolve 403 para role não-ADMIN.

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

- Cada linha de domínio pertence a um dono (`user_id`; no `fitness`, `student_id`);
  nenhum usuário vê dado de outro.
- No serviço `budgets` o isolamento é feito por filtro Hibernate (`userFilter`) + queries
  `findByIdAndUserId`; no `finances` as entidades têm `owner_id`/`user_id` nas queries;
  em `study`/`tasks` as queries são sempre por usuário do token.
- No `fitness`, o plano (treino, agenda e refeição) pertence ao **aluno** (`student_id`).
  O usuário do token é o próprio aluno ou um profissional com vínculo ativo; consultas
  usam o aluno-alvo validado, nunca um id arbitrário do payload. Escrita exige role do
  domínio + vínculo; aluno com profissional ativo no domínio é somente leitura. Config e
  acompanhamento diário são privados do aluno.
- `@Filter` **não** vale para `findById`: leituras por id sempre usam variante por usuário.
- Bulk updates precisam repetir o filtro do dono (`AND user_id = :userId`; no `fitness`,
  `AND student_id = :studentId`).
- Requisição sem `X-User-ID` em rota privada = 401 (falha fechada).
- O `budgets` usa Spring Security stateless: as rotas privadas exigem o `X-User-ID`
  injetado pelo gateway (saúde, Swagger e docs são públicas). Os demais serviços de
  produto aplicam o mesmo requisito por filtro próprio. Em todos, a porta do serviço
  não pode ser publicada no host (só via gateway).

## 6. Exclusão de dados

- Exclusões de negócio são **soft delete** (`status = DELETED` + `@SQLRestriction`).
- Exclusão via `DELETE`; o `PUT` rejeita `DELETED` com 400 em todas as entidades com status.
- Registros excluídos continuam no banco para histórico/auditoria.
- **Exceção consciente**: a solicitação de exclusão de dados do titular (LGPD, só por
  e-mail) faz **hard delete** via `lgpd_purge_user` apenas nos fluxos de purga
  (`scripts/purge-user`); todo o resto continua soft delete.

## 7. Links e páginas centrais

- Domínio único servido pelo nginx: landing em `/app/`, política de privacidade em
  `/app/privacidade/`, reset de senha em `/user/reset-password`.
- E-mails transacionais usam `SERVER_URL` como base; em produção precisa ser o domínio
  público (HTTPS).
- A política de privacidade é única para todos os produtos e cobre LGPD/Loja.

## 7.1 Bancos de dados

- `init.sql` só cria os databases e **não roda** em volume existente; vale apenas para
  ambientes novos. Ambientes existentes são atualizados pelos scripts de `migrations/`
  (o rename `crm`/`orcamentos` → `budgets` já foi executado em produção e local).

## 8. Observabilidade e auditoria

- Auditoria de operações administrativas (create/update/delete/status/confirmação/
  reenvio/invalidação de sessão) é registrada em log, sem tabela própria.
- Dados sensíveis (senha, tokens, valores financeiros) não entram em log nem em OpenAPI.

## 9. Marca dos produtos

- Cada produto tem nome próprio curto, igual na web e no app: Estudos, Finanças,
  Fitness, Orçamentos, Compras e Admin. Identificadores internos (pacotes Android,
  `applicationId`, nomes de serviço e de repositório) **não** acompanham o nome.
- Todo cliente cita a marca-mãe: "Desenvolvido por EHS Solutions" no rodapé, sidebar
  ou drawer, e a landing `/app/` apresenta os produtos como apps da EHS Solutions.
- Cor de marca da plataforma: indigo `#4F46E5` (dark `#818CF8`); cada app usa o ícone
  próprio e não reaproveita o de outro produto.
