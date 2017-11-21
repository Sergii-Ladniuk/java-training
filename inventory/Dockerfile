FROM java:8-jre-alpine

ADD target/inventory.jar inventory.jar
EXPOSE 8080
CMD java -jar -Dspring.profiles.active=docker -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 apis-entity-manager.jar
MAINTAINER sergiy-l@level11.com
