#!/bin/bash
# =============================================================================
# Script de inicialização do PostgreSQL
# =============================================================================
# Este script corre automaticamente quando o container PostgreSQL arranca
# pela primeira vez. Cria uma base de dados separada para cada microserviço.
#
# Porquê uma DB por serviço?
# → Cada serviço é dono dos seus dados. Não partilham tabelas.
# → Permite escalar, migrar ou substituir um serviço independentemente.
# → Princípio: database-per-service pattern em microserviços.
# =============================================================================

set -e

# Lê a variável POSTGRES_MULTIPLE_DATABASES e cria cada DB
if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    echo "Creating multiple databases: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        echo "  → Creating database: $db"
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
            CREATE DATABASE $db;
            GRANT ALL PRIVILEGES ON DATABASE $db TO $POSTGRES_USER;
EOSQL
    done
    # Keycloak precisa também da sua DB
    echo "  → Creating database: keycloak_db"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE keycloak_db;
        GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO $POSTGRES_USER;
EOSQL
    echo "Databases created successfully."
fi
