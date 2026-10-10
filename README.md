# SayLess

Say what you mean, in the tone you want. Enter a draft, pick a tone (Professional, Casual, Diplomatic, Funny), and get three rewrites from Google Gemini.

- `frontend/` — HTML/CSS/JS page
- `backend/` — Spring Boot API (`POST /api/rewrite`) that calls Gemini and also serves the frontend
- `docs/` — PRD-level plans, the backend contract and per-person handoffs

## Requirements

- Java 21 (newer JDKs also build it; CI uses 21)
- No Maven install needed: use the wrapper (`./mvnw` on macOS/Linux/Git Bash, `mvnw.cmd` on Windows cmd/PowerShell)

## Run it

From `backend/`:

```sh
./mvnw clean verify          # build and run all tests (no key needed)
./mvnw spring-boot:run       # start on http://localhost:8080
```

Open http://localhost:8080/. With no key, the app runs in **disabled mode**: the page loads, and rewriting shows "Rewriting isn’t available yet" (HTTP 503 `AI_NOT_CONFIGURED`). It never shows fake rewrites.

### Real Gemini rewrites

Get a free API key from Google AI Studio, then set environment variables in your shell or IDE run configuration (Spring Boot does not read `.env` files). Never commit the key or paste it in chat.

Git Bash / macOS / Linux:

```sh
export SAYLESS_AI_ENABLED=true
export GEMINI_API_KEY=...      # your key
./mvnw spring-boot:run
```

PowerShell:

```powershell
$env:SAYLESS_AI_ENABLED = "true"
$env:GEMINI_API_KEY = "..."    # your key
.\mvnw.cmd spring-boot:run
```

All settings and defaults are listed in `backend/.env.example`. If enabled mode is misconfigured (blank key, bad URL, bad timeouts), startup fails with a message naming the setting, never its value.

### Packaged JAR

```sh
./mvnw clean verify
java -jar target/sayless-backend-0.0.1-SNAPSHOT.jar
```

The JAR contains the frontend, so it runs from any directory. Rebuild after editing `frontend/`.

## Modes

| What you see | Mode |
|---|---|
| Three rewrites that match your message | Live Gemini (`SAYLESS_AI_ENABLED=true`) |
| "Rewriting isn’t available yet" | Disabled (default, no key) |
| "Demo mode" notice and the same three sample rewrites every time | Mock: `rewrite-mode` meta in `frontend/index.html` set to `mock` (frontend-only development) |

## Frontend-only development

```sh
python -m http.server 5500 --bind 127.0.0.1 --directory frontend
```

- Output tests: http://127.0.0.1:5500/tests/output-tests.html
- Output preview: http://127.0.0.1:5500/dev-output.html
- To call the backend from this server, temporarily set the `rewrite-endpoint` meta to `http://localhost:8080/api/rewrite`. The backend allows `http://localhost:5500` and `http://127.0.0.1:5500` (`SAYLESS_ALLOWED_ORIGINS`). Restore `/api/rewrite` before building.

## Known limits

- Free-tier Gemini quota is shared per Google project; heavy testing can hit 429, shown as "Rewriting is busy".
- Google may use free-tier messages to improve its products; the page says so. SayLess itself stores nothing.
- The API has no authentication or rate limiting. It's meant for a local or controlled demo, not the public internet.
