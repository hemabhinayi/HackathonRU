# Handoff — Person 1

Person / responsibility: Person 1 — shared foundation, HTTP endpoint, error mapping, configuration, CORS, build setup

Contract version and foundation SHA used: contract v1.0; foundation not yet committed (SHA pending — Person 4 records it once committed)

Branch / implementation commit: local branch `backend-p1` (from `main` at `32d1266`); uncommitted

## Files changed

Production (`backend/src/main/java/com/sayless/`):
- `contract/`: `RewriteRequest`, `RewriteResponse`, `ApiErrorResponse`, `Tone`, `ErrorCode`, `RewriteException`, `RewriteLimits`, `RewriteService`, `AiRewriteClient`
- `config/`: `AiProperties`, `CorsProperties`, `AiConfiguration` (disabled client), `WebConfiguration` (CORS + strict JSON)
- `api/`: `RewriteController`, `ApiExceptionHandler`

Tests (`backend/src/test/java/com/sayless/`): `api/RewriteControllerTest`, `config/WebConfigurationTest`, `config/AiConfigurationTest`, `contract/ContractTypesTest`

Other: `backend/pom.xml` (frontend → `static/` resource mapping), `backend/src/main/resources/application.properties`, `backend/.gitignore`, new root `.gitignore`, new `backend/.env.example`

## Public interface/config provided

- Shared types exactly as contract §5. `Tone.fromWireValue` is exact-match only; throws `RewriteException(VALIDATION_ERROR)`.
- `AiProperties(enabled, apiKey, model, baseUrl, connectTimeout, requestTimeout)` with redacted `toString()`. Validation runs only when `enabled=true`; failure messages name the property, never values.
- Disabled mode (`sayless.ai.enabled=false`, default): `AiConfiguration` registers the only `AiRewriteClient`, which throws `AI_NOT_CONFIGURED`.
- Strict JSON: numbers/booleans/arrays/objects are not coerced into strings; trailing tokens rejected; unknown fields ignored. Applies app-wide through a `JsonMapperBuilderCustomizer`.
- CORS: `/api/**`, exact origins from `SAYLESS_ALLOWED_ORIGINS`, POST/OPTIONS, `Content-Type`, no credentials, wildcards rejected at startup.
- Packaging: `index.html`, `styles.css`, `script.js`, `output.js`, `mock-response.js` copied from `../frontend` to `target/classes/static/` (filtering off). Verified the copy happens; JAR entries not yet checked.

## Behavior completed

- `POST /api/rewrite` → calls `RewriteService.rewrite(message, tone)` once with raw strings, returns `{"rewrites": [...]}`, 200, `Cache-Control: no-store`.
- Errors use the `{"error":{"code","message"}}` envelope with canonical messages and `no-store`: `RewriteException` → its code; malformed/type-invalid body → 400 `INVALID_JSON`; non-JSON content type → 415; GET etc. → 405 with `Allow`; anything else → sanitized 500.
- Logs contain only error code, status and exception class name — never messages or request content.
- The controller intentionally has no `consumes`, so a 415 still carries CORS headers for allowed origins.

## Tests run

From `backend/`:

```sh
./mvnw -q -Dtest='RewriteControllerTest,WebConfigurationTest,AiConfigurationTest,ContractTypesTest' -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: 74 tests, 0 failures (RewriteControllerTest 37, AiConfigurationTest 19, ContractTypesTest 11, WebConfigurationTest 7).

Foundation gate (before the controller was added): `./mvnw -q clean test` passed, including the existing `SaylessBackendApplicationTests` with no Gemini key.

## Tests not run, with reason

- `SaylessBackendApplicationTests` (full boot) **currently fails** on this branch: `RewriteController` needs a `RewriteService` bean, which is Person 2's `DefaultRewriteService`. Expected per the plan; do not add a placeholder service. Passes once Person 2's branch is merged.
- `./mvnw clean verify` on the combined branches — pending merge.
- JAR content check and live Gemini calls — Person 4 / Person 3.
- Built with local JDK 26 targeting Java 21; a Java 21 JDK clean build is not yet verified.

## Live provider called?

No.

## Known limitations

- Tracked `backend/target/**` files (12) are still in the Git index. Cleanup command for the dedicated commit: `git rm -r --cached -- backend/target` (leaves files on disk).
- Spring Boot does not read `.env`; variables must be exported or set in the IDE.

## Dependencies needed from another person

- **Person 2**: `service.DefaultRewriteService` as the single `RewriteService` bean, constructor taking `AiRewriteClient`, throwing `RewriteException(VALIDATION_ERROR)` for missing/null/blank/too-long message or bad tone (use `Tone.fromWireValue`).
- **Person 3**: `provider.GeminiRewriteClient` annotated `@Component` + `@ConditionalOnBooleanProperty("sayless.ai.enabled")`, constructor-injecting `AiProperties`. Nothing else may register an `AiRewriteClient`.
- **Person 4**: frontend `rewrite-mode` meta still `mock`; switch to `api` for live integration.

## Shared changes requiring coordination

None. All signatures, property names and env variables match contract v1.0.

Ready for: isolated review — yes; integrated tests — after Person 2 merges; live demo — after Persons 2 and 3 merge.
