FROM alibabadragonwell/dragonwell:21-ubuntu
LABEL author="netbuffer"
WORKDIR /
COPY target/xxl-job-demo.jar /app.jar
ENV SERVER_PORT=8080
EXPOSE 8080 9999
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]
