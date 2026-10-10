# Person 4 — integration, frontend connection and release checks

## Give your agent this task

> Implement Person 4's integration work using `docs/backend-plan.md`, `docs/backend/contract.md`, `docs/backend/gemini.md`, `docs/backend/integration.md`, this brief, current frontend/backend code, and any applicable repository instructions. Coordinate the four branches using the frozen foundation. Own integrated tests, focused frontend connection changes, CI and run instructions. Preserve other owners' implementations. Validate the combined packaged app and write `docs/backend/handoff-person-4.md`. Distinguish local fixture tests from live Gemini results. Do not enable billing or invent missing service/provider code.

## Ownership and role

Own `backend/src/test/java/com/sayless/integration/**`, the existing `SaylessBackendApplicationTests.java`, `frontend/index.html`, narrowly scoped request/error integration in `frontend/script.js`, new input tests if needed, `README.md`, `.github/workflows/backend.yml`, the integration runbook and decision record. Keep the existing output module, stylesheet and output tests working.

You are the merge/integration coordinator, not the automatic owner of every defect. Send each failing boundary to its implementation owner with the failing case and expected contract. Obtain one agreed change to shared code rather than adding a test-only production workaround.

## Work before the other branches arrive

1. Record current baseline, foundation commit once available, branch names and contract version in `decisions.md`.
2. Prepare integrated request/response cases from the contract. Reuse synthetic fixtures, but ensure tests exercise the real controller → real service → real provider adapter where indicated.
3. Prepare a local fixture server for Gemini-shaped HTTP responses, using an ephemeral port. Your integration helper lives under `integration/`; you may read Person 3's committed fixture files without editing them. No duplicate production client.
4. Prepare the README/run commands and CI using Java 21 plus the Maven wrapper. CI must run without `GEMINI_API_KEY` and must never activate live AI. Choose supported GitHub action versions when implementing and pin them consistently; this plan does not guess a future action SHA.
5. Keep the frontend in its current explicit mock mode until the packaged API flow is ready. You can test API-mode behavior using a test-served page variant without prematurely declaring a working live demo.

## Integrated backend tests

Implement `integration/RewriteFlowTest.java` with the real application on a random port and the real Gemini adapter directed to the loopback fixture server. Set `sayless.ai.enabled=true`, fake key, short test timeout, and the loopback base URL through test properties. Do not replace the adapter with a mock in this suite. Assert outbound call counts and actual HTTP envelopes/statuses.

Implement `integration/DisabledModeTest.java` (or equivalent context coverage) proving no-key boot in disabled mode and a valid rewrite request returning 503 `AI_NOT_CONFIGURED`, never a sample result. The existing context-load test remains meaningful and independent of real credentials.

Cover valid all-tone requests; invalid bodies/semantic input with zero provider calls; provider 429/5xx/timeouts; blocked/malformed/truncated output; duplicate rewrites rejected by the real service; success after a prior failure; CORS preflight and allowed-origin error responses. See the runbook for the full acceptance matrix.

Use names ending in `Test`/`Tests`, so `./mvnw clean verify` runs them without new Failsafe configuration. Prevent ambient developer environment variables from enabling live AI in any default test. Test beans belong only under test sources.

## Frontend connection

The request implementation already exists; adopt it instead of writing a new form flow. After the integrated API is verified:

- Set `frontend/index.html` meta `rewrite-mode` to **`api`** and retain meta `rewrite-endpoint` as **`/api/rewrite`** for the packaged same-origin app.
- Keep `renderResults(data?.rewrites)`, the four lowercase tones, empty-message handling, duplicate-submit guard, control recovery and 30-second browser timeout.
- Add the agreed 2,000 UTF-16-unit normalized-message check in the submit handler before any request. Preserve the full draft on failure; explain the limit beside the field. Avoid silent textarea truncation. Client validation supplements server validation.
- Map recognized backend error **codes** to a fixed local message allowlist, including quota, refusal and timeout. For unknown codes, non-JSON failures or network errors, keep a generic friendly error. Never directly display a provider/raw server message.
- Replace the live privacy note with the Gemini free-tier wording and terms link from `gemini.md`. Ensure the existing mock notice remains accurate whenever mock mode is explicitly enabled for development.
- Preserve the existing results/copy UI, keyboard focus, form IDs and selected tone. Do not add persistence, analytics or provider credentials.

If adding browser-native input tests, isolate test substitutions and restore `fetch`/clipboard state. Use a test-served API-mode page or equivalent harness so no real Gemini request occurs. Verify request JSON, size/blank validation, each tone, pending guard, response parsing and recovery. Keep the existing output test page intact.

## Packaging and CI

Person 1 owns POM static-resource copying; you verify it using the packaged JAR. Check that only runtime assets appear under `BOOT-INF/classes/static/`, normal application/prompt resources remain present, and no tests/previews/key files are packaged. Run the JAR from a directory outside the repository: it must not need a relative path to `frontend/` or a prompt source file on disk.

CI runs a clean wrapper build with Java 21 and no live key. Default unit/integration suites use fixtures. Keep browser checks separate if the runner lacks browser infrastructure, and record the manual command/results; do not claim CI runs tests it does not execute. README includes Java requirement, no-key behavior, real-key environment setup without a literal key, packaging, frontend test URLs, CORS development instructions and known free-tier limits.

## Merge and release sequence

Use the master's ordered merge gates. Review each branch's file list and handoff before combining. Run all tests after shared config or interface changes. Verify the packaged app first with a local provider fixture, then with the real free Gemini project once Person 3's probe passes. Coordinate live requests sequentially to avoid quota waste.

The final [runbook checklist](integration.md) must show separately: automated tests, browser tests, provider-access probe, live tone/intent matrix, and known limitations. If quota or account access blocks live verification, retain that as pending; do not report the full MVP as complete. After the implementation lands, update historical frontend handoff references to link to current run instructions without overwriting the original verification record.
