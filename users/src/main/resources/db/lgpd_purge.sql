CREATE OR REPLACE FUNCTION lgpd_purge_user(p_user_id uuid, p_email text DEFAULT NULL)
RETURNS TABLE(tabela text, excluidos bigint)
LANGUAGE plpgsql
SET search_path = public
AS $fn$
DECLARE
    v_count bigint;
    v_missing text;
    v_children text;
    v_roles_table text;
    v_roles_user_column text;
    v_roles_value_column text;
    v_covered text[];
    v_company_id uuid;
    v_company_name text;
    v_company_status text;
    v_new_owner uuid;
    v_companies_deleted bigint := 0;
BEGIN
    IF p_user_id IS NULL THEN
        RAISE EXCEPTION 'lgpd_purge_user: p_user_id é obrigatório';
    END IF;

    SELECT child.relname, a.attname
      INTO v_roles_table, v_roles_user_column
      FROM pg_constraint con
      JOIN pg_class parent ON parent.oid = con.confrelid
      JOIN pg_class child ON child.oid = con.conrelid
      JOIN pg_namespace pn ON pn.oid = parent.relnamespace
      JOIN pg_namespace cn ON cn.oid = child.relnamespace
      JOIN pg_attribute a ON a.attrelid = con.conrelid AND a.attnum = ANY (con.conkey)
     WHERE con.contype = 'f'
       AND pn.nspname = 'public'
       AND cn.nspname = 'public'
       AND parent.relname = 'users'
       AND child.relname <> 'companies'
       AND a.attname IN ('user_id', 'users_id')
     LIMIT 1;

    IF v_roles_table IS NULL THEN
        RAISE EXCEPTION 'lgpd_purge_user: tabela de roles com FK para users não encontrada';
    END IF;

    SELECT a.attname
      INTO v_roles_value_column
      FROM pg_attribute a
     WHERE a.attrelid = v_roles_table::regclass
       AND a.attnum > 0
       AND NOT a.attisdropped
       AND a.attname <> v_roles_user_column
     ORDER BY CASE WHEN a.attname IN ('roles', 'role') THEN 0 ELSE 1 END, a.attnum
     LIMIT 1;

    v_covered := ARRAY['users', 'companies', v_roles_table];

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

    IF v_roles_value_column IS NOT NULL THEN
        EXECUTE format('SELECT count(*) FROM %I WHERE %I = $1 AND %I = ''ADMIN''',
                       v_roles_table, v_roles_user_column, v_roles_value_column)
           INTO v_count USING p_user_id;
        IF v_count > 0 THEN
            RAISE EXCEPTION 'lgpd_purge_user: usuário possui role ADMIN; remova a role antes de excluir';
        END IF;
    END IF;

    CREATE TEMP TABLE IF NOT EXISTS _lgpd_purge_counts (
        tabela text PRIMARY KEY,
        excluidos bigint NOT NULL
    ) ON COMMIT DROP;
    TRUNCATE _lgpd_purge_counts;

    FOR v_company_id, v_company_name, v_company_status IN
        SELECT id, name, status::text FROM companies WHERE owner_id = p_user_id
    LOOP
        IF v_company_status = 'DELETED' THEN
            UPDATE users SET company_id = NULL WHERE id = p_user_id;
            UPDATE companies SET owner_id = NULL WHERE id = v_company_id;
            RAISE NOTICE 'lgpd_purge_user: empresa % já estava excluída; owner_id anulado', v_company_id;
        ELSE
            SELECT u.id
              INTO v_new_owner
              FROM users u
             WHERE u.company_id = v_company_id
               AND u.id <> p_user_id
               AND u.status <> 'DELETED'
               AND NOT EXISTS (SELECT 1 FROM companies c WHERE c.owner_id = u.id)
             ORDER BY u.created_date ASC NULLS LAST, u.id
             LIMIT 1;

            IF v_new_owner IS NOT NULL THEN
                UPDATE companies SET owner_id = v_new_owner WHERE id = v_company_id;
                UPDATE users SET company_id = NULL WHERE id = p_user_id;
                RAISE NOTICE 'lgpd_purge_user: empresa % (%) transferida para %',
                    v_company_id, v_company_name, v_new_owner;
            ELSIF EXISTS (
                SELECT 1 FROM users u
                 WHERE u.company_id = v_company_id
                   AND u.id <> p_user_id
                   AND u.status <> 'DELETED'
            ) THEN
                RAISE EXCEPTION 'lgpd_purge_user: empresa % tem outros usuários ativos, mas nenhum pode assumir a titularidade (já são donos de outra empresa); resolva manualmente', v_company_id;
            ELSE
                UPDATE users SET company_id = NULL WHERE company_id = v_company_id;
                DELETE FROM companies WHERE id = v_company_id;
                v_companies_deleted := v_companies_deleted + 1;
                RAISE NOTICE 'lgpd_purge_user: empresa % (%) removida sem outros usuários ativos',
                    v_company_id, v_company_name;
            END IF;
        END IF;
    END LOOP;

    UPDATE users SET company_id = NULL WHERE id = p_user_id AND company_id IS NOT NULL;

    IF v_companies_deleted > 0 THEN
        INSERT INTO _lgpd_purge_counts VALUES ('companies', v_companies_deleted);
    END IF;

    EXECUTE format('DELETE FROM %I WHERE %I = $1', v_roles_table, v_roles_user_column) USING p_user_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES (v_roles_table, v_count);

    DELETE FROM users WHERE id = p_user_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('users', v_count);

    RETURN QUERY SELECT c.tabela, c.excluidos FROM _lgpd_purge_counts c ORDER BY c.tabela;
END;
$fn$;
