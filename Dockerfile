# ---- Etapa 1: compilar el proyecto con Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copiamos primero solo el pom.xml para aprovechar el cache de capas de Docker:
# si no cambian las dependencias, no se vuelven a descargar en cada build.
COPY pom.xml .
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q clean package -DskipTests

# ---- Etapa 2: imagen final, solo con el JRE (mucho más liviana) ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# El * cubre cualquier nombre/version real del jar generado
# (ej. backend-0.0.1-SNAPSHOT.jar), sin tener que escribirlo a mano.
COPY --from=build /app/target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

# Render inyecta la variable PORT en runtime; application-prod.properties
# ya usa server.port=${PORT:8080}, así que esto simplemente documenta
# el puerto por defecto para cuando se corre local con 'docker run'.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
