FROM azul/zulu-openjdk:21.0.10

WORKDIR /opt/mini-go-gateway

COPY target/mini-go-gateway.jar app.jar

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 9000
STOPSIGNAL SIGTERM

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /opt/mini-go-gateway/app.jar"]
