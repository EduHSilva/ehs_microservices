-- Origem, prioridade e cliente das tarefas (lembretes do Orçamentos).
--
-- O serviço `tasks` passou a ser compartilhado pelo StudyFlow e pelo Orçamentos: cada
-- tarefa carrega a aplicação que a criou (`source`) e cada GET responde somente com as
-- tarefas da aplicação do token. Tarefas anteriores à integração vieram do StudyFlow.
--
-- O `ddl-auto=update` cria as colunas em ambientes onde o serviço sobe com a imagem nova;
-- este script deixa o mesmo resultado explícito para bancos atualizados à mão.

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS source VARCHAR(20) NOT NULL DEFAULT 'STUDY';
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS client_id UUID;

-- Backfill: tudo o que já existia é do StudyFlow (o vínculo com o CRM é novo).
UPDATE tasks SET source = 'STUDY' WHERE source IS NULL;
UPDATE tasks SET priority = 'MEDIUM' WHERE priority IS NULL;

-- Índice do filtro por dono + origem usado em toda leitura.
CREATE INDEX IF NOT EXISTS idx_tasks_user_source ON tasks (user_id, source);
