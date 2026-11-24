CREATE OR REPLACE FUNCTION lgpd_purge_user(p_user_id uuid, p_email text DEFAULT NULL)
RETURNS TABLE(tabela text, excluidos bigint)
LANGUAGE plpgsql
SET search_path = public
AS $fn$
DECLARE
    v_count bigint;
    v_missing text;
    v_children text;
    v_shared text;
    v_covered text[] := ARRAY['category', 'credit_card', 'finance_account',
                              'finance_account_member', 'goal_contribution', 'monthly_goal',
                              'recurrences', 'transaction', 'transaction_import_audit',
                              'user_preference'];
BEGIN
    IF p_user_id IS NULL THEN
        RAISE EXCEPTION 'lgpd_purge_user: p_user_id é obrigatório';
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

    SELECT string_agg(DISTINCT a.name, ', ')
      INTO v_shared
      FROM finance_account a
      JOIN finance_account_member m ON m.account_id = a.id
     WHERE a.owner_id = p_user_id
       AND m.status = 'ACTIVE'
       AND (m.user_id IS NULL OR m.user_id <> p_user_id)
       AND (p_email IS NULL OR lower(m.email) <> lower(p_email));

    IF v_shared IS NOT NULL THEN
        RAISE EXCEPTION 'lgpd_purge_user: contas com membros ativos: %. Revogue ou transfira os membros antes de excluir.', v_shared;
    END IF;

    CREATE TEMP TABLE IF NOT EXISTS _lgpd_purge_counts (
        tabela text PRIMARY KEY,
        excluidos bigint NOT NULL
    ) ON COMMIT DROP;
    TRUNCATE _lgpd_purge_counts;

    DELETE FROM transaction_import_audit
     WHERE user_id = p_user_id
        OR transaction_id IN (
            SELECT t.id FROM "transaction" t
             WHERE t.user_id = p_user_id
                OR t.account_id IN (SELECT a.id FROM finance_account a WHERE a.owner_id = p_user_id)
        );
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('transaction_import_audit', v_count);

    DELETE FROM goal_contribution
     WHERE user_id = p_user_id
        OR goal_id IN (
            SELECT g.id FROM monthly_goal g
             WHERE g.user_id = p_user_id
                OR g.account_id IN (SELECT a.id FROM finance_account a WHERE a.owner_id = p_user_id)
        );
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('goal_contribution', v_count);

    DELETE FROM "transaction" t
     WHERE t.user_id = p_user_id
        OR t.account_id IN (SELECT a.id FROM finance_account a WHERE a.owner_id = p_user_id);
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('transaction', v_count);

    DELETE FROM recurrences r
     WHERE r.user_id = p_user_id
        OR r.account_id IN (SELECT a.id FROM finance_account a WHERE a.owner_id = p_user_id);
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('recurrences', v_count);

    DELETE FROM credit_card c
     WHERE c.user_id = p_user_id
        OR c.account_id IN (SELECT a.id FROM finance_account a WHERE a.owner_id = p_user_id);
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('credit_card', v_count);

    DELETE FROM monthly_goal g
     WHERE g.user_id = p_user_id
        OR g.account_id IN (SELECT a.id FROM finance_account a WHERE a.owner_id = p_user_id);
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('monthly_goal', v_count);

    DELETE FROM category WHERE user_id = p_user_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('category', v_count);

    DELETE FROM finance_account_member m
     WHERE m.user_id = p_user_id
        OR (p_email IS NOT NULL AND lower(m.email) = lower(p_email))
        OR m.account_id IN (SELECT a.id FROM finance_account a WHERE a.owner_id = p_user_id);
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('finance_account_member', v_count);

    DELETE FROM finance_account a WHERE a.owner_id = p_user_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('finance_account', v_count);

    DELETE FROM user_preference WHERE user_id = p_user_id;
    GET DIAGNOSTICS v_count = ROW_COUNT;
    INSERT INTO _lgpd_purge_counts VALUES ('user_preference', v_count);

    RETURN QUERY SELECT c.tabela, c.excluidos FROM _lgpd_purge_counts c ORDER BY c.tabela;
END;
$fn$;
