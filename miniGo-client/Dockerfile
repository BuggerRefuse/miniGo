FROM azul/zulu-openjdk:21.0.10

WORKDIR /opt/minigo-client

COPY miniGo-client-bootstrap/target/miniGo-client-bootstrap-1.0-SNAPSHOT.jar app.jar

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8081
STOPSIGNAL SIGTERM

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/minigo-client/app.jar"]
