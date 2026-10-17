# Supabase backend configuration

## Container deployment

The repository includes Dockerfiles for the Spring Boot API and React frontend. The frontend's Nginx container serves the single-page app and proxies `/api` calls to the backend over the private Compose network; only the frontend port is exposed.

1. Copy `.env.example` to `.env` and set `SUPABASE_DB_URL`, `SUPABASE_DB_USER`, and `SUPABASE_DB_PASSWORD`. Add at least one of `TAVILY_API_KEY`, `EXA_API_KEY`, or `SERPER_API_KEY` to enable lead collection.
2. Run `docker compose up --build -d` from the project root.
3. Open `http://localhost:8085`, or the host port set by `APP_PORT`.
4. Check backend output with `docker compose logs -f backend` and stop the containers with `docker compose down`.

For a public VPS deployment, configure a domain and HTTPS reverse proxy to the frontend service. Store the `.env` values in the host's secret manager where available. Never commit `.env` or put backend keys in Vite variables.

**Access control:** this project does not currently implement user login or authorization. Keep the deployment private or protect it with an access-control layer before exposing it publicly; otherwise visitors can access lead data and collection actions.

The backend accepts the hosting platform's `PORT` variable and defaults to port 8080. The database schema setting can be controlled by `JPA_DDL_AUTO` and defaults to `update` for an initially empty Supabase database.

The backend now requires database credentials from environment variables; do not put the database password in source control.

Configure these on the backend host (or in the local process environment):

- `SUPABASE_DB_URL`: JDBC URL from the Supabase **Session pooler** connection details, in the form `jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require`.
- The backend limits its Hikari connection pool to 3 (1 idle) by default to stay within Supabase Session Pooler limits during rolling deploys. `DB_POOL_MAX_SIZE` and `DB_POOL_MIN_IDLE` can override these defaults if needed.
- `SUPABASE_DB_USER`: pooler database user, typically `postgres.<project-ref>`.
- `SUPABASE_DB_PASSWORD`: the database password from the Supabase project.
- `SUPABASE_DB_DRIVER`: optional; defaults to `org.postgresql.Driver`.

For LinkedIn public search, also configure `TAVILY_API_KEY`, `EXA_API_KEY`, and/or `SERPER_API_KEY` as backend secrets. Never use a `VITE_` prefix for backend secrets.

For local PowerShell runs, set the variables in the current terminal before starting `./mvnw.cmd spring-boot:run`. For deployment, add them to the backend service's environment/secrets settings and redeploy. Keep `.env` files out of source control.

After the backend starts, verify the actual database connection from that backend process:

```powershell
Invoke-RestMethod http://localhost:8080/api/system/status |
  Select-Object -ExpandProperty database |
  Select-Object connected, isSupabase, databaseProduct, mode
```

For Supabase, `connected` and `isSupabase` must both be `True`, and `databaseProduct` should report PostgreSQL. `isSupabase` is based on a successful JDBC connection, PostgreSQL metadata, and a Supabase database hostname; configured environment variables alone do not count as a connection.
