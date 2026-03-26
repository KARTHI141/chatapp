#!/bin/sh

# Convert Render's DATABASE_URL (postgres://user:pass@host:port/db)
# to Spring Boot JDBC format (jdbc:postgresql://host:port/db) + separate credentials
if [ -n "$DATABASE_URL" ]; then
  # Remove scheme prefix (postgres:// or postgresql://)
  DB_URL="${DATABASE_URL#*://}"

  # Extract user:password (before @)
  DB_USERINFO="${DB_URL%%@*}"
  DB_USERNAME="${DB_USERINFO%%:*}"
  DB_PASSWORD="${DB_USERINFO#*:}"

  # Extract host:port/database (after @)
  DB_HOSTPATH="${DB_URL#*@}"

  export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOSTPATH}"
  export SPRING_DATASOURCE_USERNAME="${DB_USERNAME}"
  export SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}"
fi

exec java -jar app.jar
