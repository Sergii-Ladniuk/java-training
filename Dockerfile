FROM mysql

# Environment variables
ENV MYSQL_ROOT_PASSWORD testpwd

# Create Database
RUN	mkdir /usr/sql
RUN	chmod 644 /usr/sql

ADD ["inventory/src/main/resources/db.sql", "/usr/sql/db.sql"]

RUN /etc/init.d/mysql start && \
    	mysql -h localhost -uroot -p${MYSQL_ROOT_PASSWORD} < /usr/sql/db.sql

EXPOSE 3306

