FROM mysql

ADD ["inventory/src/main/resources/db.sql", "/docker-entrypoint-initdb.d/db.sql"]

EXPOSE 3306

