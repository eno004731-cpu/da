FROM eclipse-temurin:17-jdk AS builder
# Стадия сборки: собираем jar внутри контейнера,
# чтобы не зависеть от Java/Maven на сервере.

WORKDIR /build

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src

RUN chmod +x mvnw && ./mvnw -DskipTests package

FROM eclipse-temurin:17-jre
# Стадия запуска: оставляем только JRE и готовый jar,
# чтобы образ был легче и безопаснее.

WORKDIR /app

COPY --from=builder /build/target/demo-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/uploads/orders

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
