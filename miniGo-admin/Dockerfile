FROM azul/zulu-openjdk:21.0.10

WORKDIR /opt/mini-go-admin

COPY miniGo-admin-bootstrap/target/miniGo-admin-bootstrap-1.0-SNAPSHOT.jar app.jar

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8082
STOPSIGNAL SIGTERM

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/mini-go-admin/app.jar"]
