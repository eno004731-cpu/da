SELECT 'CREATE DATABASE legal_crm'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'legal_crm'
)\gexec

SELECT 'CREATE DATABASE legal_auth'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'legal_auth'
)\gexec

SELECT 'CREATE DATABASE legal_notification'
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = 'legal_notification'
)\gexec
