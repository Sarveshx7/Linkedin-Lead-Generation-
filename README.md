# LeadForge

Application: https://leadforge-frontend.vercel.app

LeadForge is a campaign-based lead discovery dashboard by TheBoredMonkey. Its LinkedIn workflow searches public search indexes through Tavily, Exa, and Serper, then stores qualified result records in PostgreSQL (configured for Supabase in deployment).

## What the LinkedIn collector does

1. Builds a query from category-specific defaults, the selected campaign's custom keywords, and exclusions. Creator/collaboration campaigns target brands seeking creators, influencers, UGC, and paid partnerships; advertising campaigns target agency, social campaign, and creative partner opportunities.
2. Sends that query to each configured provider in parallel.
3. Keeps only provider results whose URL matches a LinkedIn post permalink pattern, merges exact duplicate URLs, and filters by campaign terms, exclusions, and commercial-intent phrases.
4. Assigns a service category and intent score, extracts possible email/phone strings from returned text, and saves the result and provider metadata.
5. The dashboard shows the source permalink, provider-returned text, any provider-supplied fields, and the app's own classification.

This is public-index discovery, not a LinkedIn integration. The collector does not log in, fetch LinkedIn post pages, call private LinkedIn endpoints, or bypass access controls. Search indexes can be delayed or incomplete. The source link should be checked before outreach.

## Data quality and field provenance

- **Title, result text, permalink, and date:** returned by a configured search provider. The result text may be an excerpt, highlight, or title fallback; it is not a verified full post.
- **Author:** retained only when the provider returns a structured author value. The app no longer guesses a person's name from a title.
- **Organization and location:** the current LinkedIn provider adapters do not populate these fields. They remain empty unless a future authorized source supplies them.
- **India market signal:** inferred by the app from result text; it is not a location supplied by LinkedIn or the search provider.
- **Email and phone:** regex matches found in provider-returned text. They may be incomplete or unrelated; verify before use.
- **Category, intent, relevance, and completeness:** app-generated classifications based on the returned title/text and available fields. Provider overlap records which search services returned the same URL; overlap does not verify the post's claims.

An illustrative, synthetic output shape is in [docs/SAMPLE_RESULTS.md](docs/SAMPLE_RESULTS.md). The collection pipeline is in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Configure and run locally (Windows PowerShell)

The backend currently requires a reachable PostgreSQL database at startup. For Supabase, use the project's Session Pooler JDBC connection details. Set these values in the backend process environment; do not commit them or expose them with a `VITE_` prefix.

```powershell
$env:SUPABASE_DB_URL = 'jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require'
$env:SUPABASE_DB_USER = 'postgres.<project-ref>'
$env:SUPABASE_DB_PASSWORD = '<your-database-password>'
$env:TAVILY_API_KEY = '<optional-provider-key>'
$env:EXA_API_KEY = '<optional-provider-key>'
$env:SERPER_API_KEY = '<optional-provider-key>'
./mvnw.cmd spring-boot:run
```

The three search keys are optional; at least one is needed to run LinkedIn discovery. Open a second terminal:

```powershell
Set-Location frontend
npm install
npm run dev
```

Vite serves on port 5173 by default (or the next port if 5173 is occupied). Its `/api` proxy targets `http://localhost:8080` by default. To override it, set `VITE_API_PROXY_TARGET` before starting Vite.

The dashboard's database indicator is based on a live backend connection check. “Supabase” is shown only when that connection succeeds, the database reports PostgreSQL, and the configured JDBC URL uses a Supabase host.

## Verification

- Frontend production bundle: `Set-Location frontend; npm run build`
- Backend tests: `./mvnw.cmd test` (the current Spring context test requires the database environment variables above because the application uses the configured datasource at startup).
- Database connectivity: start the backend with the Supabase values, then inspect **Supabase / DB** in the app or request `GET http://localhost:8080/api/system/status`. A successful response with `database.connected: true` and `database.isSupabase: true` confirms the backend can open a connection to the configured Supabase PostgreSQL database.

See [DEPLOYMENT.md](DEPLOYMENT.md) for backend environment variables and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the component diagram.

## Deploy with Docker Compose

The production frontend container serves the built React app and forwards `/api` requests to the backend container. The backend is not published directly to the host; it connects to Supabase PostgreSQL.

1. Copy `.env.example` to `.env` and fill in the Supabase database values and at least one search-provider key.
2. Start the app with `docker compose up --build -d`.
3. Open `http://localhost:8085` (or the host port set with `APP_PORT`).

See [DEPLOYMENT.md](DEPLOYMENT.md) for logs, shutdown, secret handling, and public-deployment limitations.
