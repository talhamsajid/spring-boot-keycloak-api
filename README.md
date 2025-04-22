# Spring Boot Keycloak API

A secure backend API server built with Java Spring Boot, Keycloak (OAuth2/OIDC), MongoDB, and Hibernate OGM. Features JWT-based authentication, user registration, login, and profile endpoints.

---

## Features
- **JWT Authentication** with Keycloak (OAuth2/OpenID Connect)
- **User Registration, Login, and Profile** endpoints
- **MongoDB** as the primary database (via Hibernate OGM/JPA)
- **Spring Security** for route protection
- **Swagger/OpenAPI** documentation
- **Dockerized** for easy local development

---

## Prerequisites
- Java 11 or higher (for local build)
- Docker & Docker Compose (for containerized setup)
- [Git](https://git-scm.com/)

---

## Quick Start (Docker Compose)

1. **Clone the repository:**
   ```sh
   git clone https://github.com/talhamsajid/spring-boot-keycloak-api.git
   cd spring-boot-keycloak-api
   ```

2. **Build the Spring Boot JAR:**
   ```sh
   ./gradlew clean build
   ```
   *(If you don't have Gradle installed, use the included wrapper: `./gradlew`)*

3. **Start all services:**
   ```sh
   docker-compose up --build
   ```
   This will launch:
   - MongoDB (port 27017)
   - Keycloak (port 8180)
   - Spring Boot API (port 8080)

4. **Access the API:**
   - Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - Keycloak Admin Console: [http://localhost:8180](http://localhost:8180)

---

## Local Development (without Docker)

1. **Install MongoDB** and ensure it's running on `localhost:27017`.
2. **Install and run Keycloak** (or use Docker):
   ```sh
   docker run -p 8180:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:21.1.2 start-dev --http-port=8180
   ```
3. **Configure Keycloak Realm/Client:**
   - Create a realm: `spring-boot-api-realm`
   - Create a client: `spring-boot-api-client` (confidential, with `http://localhost:8080/*` as valid redirect URI)
   - Set client secret in `application.yml` or via env var
   - Create roles: `user`, `admin`
   - Create users and assign roles
4. **Run the app:**
   ```sh
   ./gradlew bootRun
   ```

---

## API Endpoints
- `POST /auth/register` — Register a new user
- `POST /auth/login` — Login and get JWT
- `GET /auth/profile` — Get current user's profile (JWT required)
- `GET /auth/profile/{userId}` — Get user profile by ID (admin only)

See [Swagger UI](http://localhost:8080/swagger-ui.html) for full documentation and try-it-out.

---

## Environment Variables
- `SPRING_PROFILES_ACTIVE` — Set to `dev` or `prod`
- `MONGODB_URI` — MongoDB connection string
- `KEYCLOAK_URL` — Keycloak server URL
- `KEYCLOAK_REALM` — Keycloak realm
- `KEYCLOAK_CLIENT_ID` — Keycloak client ID
- `KEYCLOAK_CLIENT_SECRET` — Keycloak client secret

---

## Troubleshooting
- Ensure MongoDB and Keycloak are running and accessible.
- Check Keycloak client and realm configuration matches your app settings.
- For Docker, use `docker-compose logs` to view logs.

---

## License
MIT
