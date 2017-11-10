FROM onelouder/alpine-java-8-base

ENV HEALTHCHECK_URI http://localhost:8080/status
HEALTHCHECK --interval=15s --timeout=3s --retries=12 CMD curl --fail $HEALTHCHECK_URI || exit 1

RUN mkdir -p /app
ADD target/apis-entity-manager.jar apis-entity-manager.jar
EXPOSE 8080
CMD java -jar -Dspring.profiles.active=qa -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 apis-entity-manager.jar
MAINTAINER sergiy-l@level11.com
