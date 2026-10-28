# Backend

API de reservas de servicios construida con Spring Boot 3.3.4, Java 17 y PostgreSQL.

## Requisitos

- Java 17 o superior
- Maven 3.9 o superior
- Docker Desktop con Docker Compose

## Configuración

1. Crea una copia del archivo de ejemplo:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Edita `.env` y configura las credenciales de PostgreSQL:

   ```env
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=reservas_db
   DB_USERNAME=postgres
   DB_PASSWORD=tu_contraseña
   SERVER_PORT=8080
   DDL_AUTO=update
   ```

   El archivo `.env` está excluido de Git para evitar publicar credenciales.

3. `DDL_AUTO=update` permite crear y actualizar las tablas automáticamente durante el debugging local. Para entornos compartidos o producción, usa `DDL_AUTO=validate` y aplica el esquema mediante migraciones o scripts controlados.

## Base de datos local

Inicia PostgreSQL en un contenedor Docker. Los datos se guardan en el volumen Docker `backend-postgres-data`, no dentro del repositorio:

```powershell
docker compose up -d postgres
docker compose ps
```

Para detener el contenedor sin borrar los datos:

```powershell
docker compose stop postgres
```

Para detenerlo y eliminar también la base local:

```powershell
docker compose down -v
```

## Ejecutar

Desde la raíz del proyecto:

```powershell
mvn spring-boot:run
```

La API estará disponible en `http://localhost:8080`, o en el puerto definido por `SERVER_PORT`.

## Ejecutar las pruebas

Las pruebas usan una base de datos H2 en memoria:

```powershell
mvn test
```

## Crear y ejecutar el JAR

```powershell
mvn clean package -DskipTests
java -jar target\backend-0.0.1-SNAPSHOT.jar
```