# PING · imagen para la demostración pública (Render Web Service Free + Aiven MySQL Free).
# Etapa 1 compila con el Maven Wrapper del proyecto (sin ejecutar la suite: la CI ya la ejecuta).
# Etapa 2 solo lleva el JRE 21 y el JAR; se ejecuta sin privilegios.
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /src
# Sin unzip el wrapper descargaría el .tar.gz y su SHA-256 no coincidiría con el declarado para el .zip.
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Copias de Windows pueden traer CRLF: el wrapper compararía mal el SHA-256 de Maven.
RUN for f in mvnw .mvn/wrapper/maven-wrapper.properties; do tr -d '\015' < "$f" > "$f.lf" && mv "$f.lf" "$f"; done && chmod +x mvnw \
 && ./mvnw -B --no-transfer-progress dependency:go-offline
COPY src/ src/
RUN ./mvnw -B --no-transfer-progress -Dmaven.test.skip=true package \
 && cp target/ping-0.0.1-SNAPSHOT.jar /ping.jar

FROM eclipse-temurin:21-jre-noble
RUN groupadd --system ping && useradd --system --gid ping --home-dir /app --shell /usr/sbin/nologin ping
WORKDIR /app
COPY --from=build /ping.jar /app/ping.jar
COPY deploy/render/entrypoint.sh /app/entrypoint.sh
RUN chmod 0555 /app/entrypoint.sh && chown -R ping:ping /app
USER ping
# Memoria: 512 MB del plan gratuito. El heap usa una fracción y deja margen a metaspace, hilos, código y buffers.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55 -XX:+UseSerialGC -Xss512k -XX:ReservedCodeCacheSize=64m -XX:MaxMetaspaceSize=160m -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=mysql,render
EXPOSE 10000
ENTRYPOINT ["/app/entrypoint.sh"]
