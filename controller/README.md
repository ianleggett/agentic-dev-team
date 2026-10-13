# YAADT Controller

The controller is the first Spring Boot slice of YAADT. It currently provides
project CRUD backed by JPA, PostgreSQL, and Flyway.

## Local development

Run the isolated test suite with:

```bash
mvn test
```

Run the API against local PostgreSQL with:

```bash
docker compose up --build postgres-controller controller-api
```

The API is available at `http://localhost:8080`. Health is exposed at
`/actuator/health`, and project endpoints are under `/api/projects`.

The controller database is intentionally separate from the Beads database and
will later be separate from Temporal's persistence database as well.
