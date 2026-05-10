FROM eclipse-temurin:21-jdk
# Берём Java 21

WORKDIR /app
# Создаём рабочую папку /app внутри контейнера

COPY . .
# Копируем собранный jar-файл внутрь контейнера

EXPOSE 8080
# Указываем порт приложения

ENTRYPOINT ["java", "-jar", "app.jar"]
# Запускаем Spring Boot приложение