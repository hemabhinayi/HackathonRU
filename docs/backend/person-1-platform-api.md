# Person 1 — foundation, HTTP API and platform

## Give your agent this task

> Implement Person 1's backend work in this brief. First read `docs/backend-plan.md`, `docs/backend/contract.md`, `docs/backend/gemini.md`, current source files, and any applicable repository instructions. Preserve other owners' work. Deliver the shared foundation first; then implement the API against the frozen interfaces. Use the current Spring Boot 4.1.1 / Java 21 setup. Run the appropriate checks and write `docs/backend/handoff-person-1.md`. Do not implement Person 2's service, Person 3's Gemini client, or Person 4's frontend changes. Do not silently change a shared signature or environment variable.

## Ownership

You own `com.sayless.contract`, `com.sayless.api`, `com.sayless.config`, API/config tests, `backend/pom.xml`, main `application.properties`, ignore files, `.env.example`, and generated-file index cleanup. See the master ownership table for bootstrap/wrapper exceptions. Person 4 owns the existing context test, README and CI. Coordinate those changes rather than editing them concurrently.

## Deliverable A: foundation everyone imports

1. Inspect `git status`, the existing POM and wrapper. Record actual Java/Maven versions. Run the current backend test before adding features. Verify wrapper resolution with the existing versions; resolve an actual tooling failure deliberately rather than silently upgrading/downgrading dependencies.
2. Add exactly the shared types from contract section 5. Put each in its specified package/file. Implement strict `Tone.fromWireValue`, canonical `ErrorCode` status/messages, safe `RewriteException`, and `RewriteLimits`. Do not include provider DTOs in this package.
3. Add `AiProperties` / `CorsProperties` and property binding/defaults exactly as specified. Centralize configuration registration. In disabled mode, provide the one `AiRewriteClient` bean that throws `AI_NOT_CONFIGURED`; no outgoing call or fixture response.
4. Implement sanitized enabled-mode configuration validation. Avoid printing key values in property validation errors or record `toString()` output. Set server error options to avoid displaying exception messages/stack traces on default framework errors.
5. Configure Maven to include the five runtime frontend files under generated `static/` resources, preserving normal resources and disabling filtering. Do not copy frontend source into a second tracked folder. Test the JAR entries later with Person 4.
6. Add an empty-key `.env.example` documenting all variables from the contract and explaining that environment variables must actually be exported/set in the IDE. Ignore real `.env`, `.env.*` except `.env.example`, and build outputs.
7. Remove **only tracked `backend/target/` entries from the index**, in a dedicated cleanup change. Preview with `git ls-files backend/target`, use `git rm -r --cached -- backend/target` when committing that authorized cleanup, and leave the working directory build outputs alone. `.gitignore` alone does not untrack files. Do not run repository-wide cache removal or `git clean`.
8. Run foundation tests and produce the foundation commit/handoff through the team's chosen Git workflow. Person 4 records its SHA. The original context test must still work with no key. No controller bean yet, since Person 2's service does not exist.

**Foundation gate:** shared signatures/config compile; no Gemini key required; one disabled client; existing context test green; no duplicate DTOs; build outputs/secrets excluded. Tell all owners exactly which revision to incorporate.

## Deliverable B: API and error boundary

Create `api.RewriteController` with constructor-injected `RewriteService` and one POST mapping for `/api/rewrite`. It accepts `RewriteRequest`, calls `rewrite(message, tone)` once, and returns `RewriteResponse` with HTTP 200 and `Cache-Control: no-store`.

Configure strict JSON string typing for the request DTO: numbers/booleans must not become strings. Also reject trailing JSON, wrong root shape and null root. Use the Boot-managed JSON integration; test the actual MVC binding behavior rather than assuming annotations enforce JSON types. Ignore unknown fields as the contract specifies. Missing/null individual fields reach Person 2's semantic validation. Avoid duplicate validation rules in controller and service.

Create `api.ApiExceptionHandler` to map `RewriteException`, malformed/type-invalid request bodies, unsupported content type, unsupported methods, and unexpected application exceptions to the shared envelope/status. Preserve framework headers such as `Allow`. Use only canonical public messages. Add no-store to errors too. If using `ResponseEntityExceptionHandler`, use the actual Spring version's signatures/imports and avoid competing handlers for the same exception.

Create CORS configuration once for the documented exact origins and `/api/**`, POST/OPTIONS, Content-Type, credentials off. Ensure preflight and error responses work. Do not add both controller `@CrossOrigin` and global configuration.

Production API code should depend on shared interfaces, not `GeminiRewriteClient` or `DefaultRewriteService` directly. No controller hardcoded fake response, provider prompt, API-key lookup, or HTTP call.

## Required tests

Suggested files: `api/RewriteControllerTest.java`, `api/ApiExceptionHandlerTest.java`, `config/WebConfigurationTest.java`, `config/AiConfigurationTest.java`. Test names end in `Test`/`Tests` so the existing Maven test lifecycle discovers them.

| Test | Required assertion |
|---|---|
| Valid request with mocked service | POST binds exact string values, invokes service once, returns exactly the rewrites field/list, correct content type and no-store |
| Invalid transport input | Empty body, malformed JSON, null/array/scalar root, trailing JSON, numeric/boolean/object/array fields produce 400 `INVALID_JSON`; service not called |
| Missing/null fields | Deserialization reaches the service and mapped semantic error is 400 `VALIDATION_ERROR` |
| Unknown property | Does not change known fields or enter the provider config/prompt |
| Service failures | Every `ErrorCode` produces the exact status/envelope; unexpected exception gives sanitized 500 |
| HTTP behavior | text/plain → 415; GET on rewrite → 405 with Allow; no raw exception/request/provider text |
| CORS | Both allowed origins' preflights succeed with proper headers; disallowed origin has no allow-origin authorization; error response remains readable from allowed origin |
| Disabled config | Starts without key and selected client throws `AI_NOT_CONFIGURED`; no network |
| Invalid enabled config | Blank key/model, invalid URL and bad timeout values fail with sanitized configuration explanation |

MVC slice tests use a mocked `RewriteService`; they do not need Person 2 or Gemini. Test framework packages changed in Boot 4, so follow the linked official reference rather than adding dependencies copied from a Boot 3 tutorial.

Example targeted command, from `backend/`, after these tests exist:

```sh
./mvnw -Dtest=RewriteControllerTest,ApiExceptionHandlerTest,WebConfigurationTest,AiConfigurationTest test
```

If your chosen test split differs, record the real command. Do not claim full application boot on a controller-only branch without the service dependency. Once branches combine, `./mvnw clean verify` must pass.

## Handoff to the others

- To Person 2: shared types, validation/error contract and foundation SHA; service implementation must have one Spring bean.
- To Person 3: `AiProperties` accessors, enabled/disabled condition, model/base URL, deadline values and HTTP-client dependency availability.
- To Person 4: built artifact path, static-resource mapping, config examples, CORS origins and API error fixtures; list tests actually executed.
- Put changes to the common contract through the decision record before consumers adopt them. Finish with the [handoff template](decisions.md#handoff-template).
