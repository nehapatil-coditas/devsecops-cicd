## Minimal Spring Boot API (Dockerized)

Lightweight Java 17 Spring Boot service exposing unauthenticated APIs plus Actuator health and metrics.

- APIs: `/api/hello`, `/api/health`
- Actuator: `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`
- Build/Run via Docker (no local Maven required)

### Build and run with Docker

```bash
docker build -t java-devsecops-demo .
docker run --name java-devsecops-demo -d  -p 8084:8080 java-devsecops-demo
```

The app will be available at `http://localhost:8084`.

### Endpoints

- GET `http://localhost:8084/api/hello` → `{ "message": "Hello, world!" }`
- GET `http://localhost:8084/api/health` → `{ "status": "UP" }`
- GET `http://localhost:8084/actuator/health`
- GET `http://localhost:8084/actuator/metrics`
- GET `http://localhost:8084/actuator/prometheus`

### Optional: run locally (requires Maven)

```bash
mvn spring-boot:run
```

### Notes

- Container has a Docker `HEALTHCHECK` for `/actuator/health`.
- Default server port inside container is 8080; mapped to host 8084 above.
