DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spinnvill-opprydding-dev')
    THEN
        ALTER DEFAULT PRIVILEGES FOR USER "spinnvill" IN SCHEMA public GRANT ALL PRIVILEGES ON SEQUENCES TO "spinnvill-opprydding-dev";
        ALTER DEFAULT PRIVILEGES FOR USER "spinnvill" IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO "spinnvill-opprydding-dev";
    END IF;
END $$;

DO $$BEGIN
    IF EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'spinnvill-opprydding-dev') THEN
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "spinnvill-opprydding-dev";
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "spinnvill-opprydding-dev";
    END IF;
END$$;
