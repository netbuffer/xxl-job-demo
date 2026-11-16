#!/bin/bash
mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" --default-character-set=utf8mb4 < /docker-entrypoint-initdb.d/schema.sql.bak
