CREATE OR REPLACE FUNCTION lgpd_purge_user(p_user_id uuid, p_email text DEFAULT NULL)
RETURNS TABLE(tabela text, excluidos bigint)
LANGUAGE plpgsql
SET search_path = public
AS $fn$
DECLARE
    v_count bigint;
    v_missing text;
    v_children text;
    v_user_column text;
    v_covered text[] := ARRAY['email'];
BEGIN
    IF p_user_id IS NULL THEN
        RAISE EXCEPTION 'lgpd_purge_user: p_user_id é obrigatório';
    END IF;

    SELECT column_name
      INTO v_user_column
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name = 'email'
       AND column_name IN ('user_id', 'userid', 'user_i_d', 'users_id')
     ORDER BY CASE column_name WHEN 'user_id' THEN 0 WHEN 'userid' THEN 1 ELSE 2 END
     LIMIT 1;

    IF v_user_column IS NULL THEN
        RAISE EXCEPTION 'lgpd_purge_user: coluna de usuário não encontrada na tabela email';
    END IF;

    SELECT string_agg(DISTINCT c.table_name, ', ')
      INTO v_missing
      FROM information_schema.columns c
      JOIN information_schema.tables t
        ON t.table_schema = c.table_schema
       AND t.table_name = c.table_name
     WHERE c.table_schema = 'public'
       AND t.table_type = 'BASE TABLE'
       AND c.column_name IN ('user_id', 'users_id', 'userid', 'user_i_d', 'owner_id',
                             'student_id', 'author_id', 'professional_id', 'account_id',
                             'invited_email')
       AND c.table_name <> ALL (v_covered);

    SELECT string_agg(DISTINCT child.relname, ', ')
      INTO v_children
      FROM pg_constraint con
      JOIN pg_class parent ON parent.oid = con.confrelid
      JOIN pg_class child ON child.oid = con.conrelid
      JOIN pg_namespace pn ON pn.oid = parent.relnamespace
      JOIN pg_namespace cn ON cn.oid = child.relnamespace
     WHERE con.contype = 'f'
       AND pn.nspname = 'public'
       AND cn.nspname = 'public'
       AND child.relkind IN ('r', 'p')
       AND parent.relname = ANY (v_covered)
       AND child.relname <> ALL (v_covered);

    IF v_missing IS NOT NULL OR v_children IS NOT NULL THEN
        RAISE EXCEPTION 'lgpd_purge_user: revisar cobertura do schema (colunas de dono: %; filhas de tabela coberta: %)',
            coalesce(v_missing, '-'), coalesce(v_children, '-');
    END IF;

    CREATE TEMP TABLE IF NOT EXISTS _lgpd_purge_counts (
        tabela text PRIMARY KEY,
        excluidos bigint NOT NULL
    ) ON COMMIT DROP;
    TRUNCATE _lgpd_purge_counts;

    EXECUTE format('DELETE FROM email WHERE %I = $1', v_user_column) USING p_user_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('email', v_count);

    RETURN QUERY SELECT c.tabela, c.excluidos FROM _lgpd_purge_counts c ORDER BY c.tabela;
END;
$fn$;
