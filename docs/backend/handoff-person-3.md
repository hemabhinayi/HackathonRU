# Handoff — Person 3

Person / responsibility: Person 3 — Gemini REST client, prompt, structured-output parsing, provider tests

Contract version and foundation SHA used: contract v1.0; foundation `a4b48f7`, Person 2 service `f8ebe97`

Branch / implementation commit: local branch `backend-p3` (at `f8ebe97`); uncommitted

## Files changed

- `backend/src/main/java/com/sayless/provider/GeminiRewriteClient.java`
- `backend/src/main/resources/prompts/rewrite-system.txt`
- `backend/src/test/java/com/sayless/provider/GeminiRewriteClientTest.java`
- `backend/src/test/resources/gemini/`: `success.json`, `split-parts.json`, `blocked-prompt.json`, `blocked-candidate.json`, `truncated.json`, `malformed-model-json.json`, `wrong-result-types.json`, `duplicate-rewrites.json`

## Interface consumed

`AiRewriteClient`, `AiProperties`, `Tone`, `ErrorCode`, `RewriteException`, `RewriteLimits` from the foundation, all unchanged. Registered with `@Component` + `@ConditionalOnBooleanProperty("sayless.ai.enabled")`, so exactly one client exists in each mode.

## Behavior completed

- `POST {baseUrl}/v1beta/models/{model}:generateContent`, key in the `x-goog-api-key` header (never in the URL), body built with Jackson exactly as specified in `gemini.md` (one candidate, JSON schema with 3 strings, `maxOutputTokens` 4096, `thinkingLevel` MINIMAL). The user part is a JSON-encoded `{tone, message}`.
- Prompt is loaded once from the classpath.
- JDK `HttpClient`, no redirects, connect timeout, plus one deadline covering the whole response body; the future is cancelled on timeout or interruption, and the interrupt flag is restored.
- Mapping: 429 → `AI_RATE_LIMITED`; other non-2xx or I/O failure → `AI_UNAVAILABLE`; deadline → `AI_TIMEOUT`; `promptFeedback.blockReason` or a safety finish reason → `AI_REFUSED`; anything else malformed, truncated (`MAX_TOKENS`), non-`STOP`, Markdown-wrapped, wrong types or extra fields → `AI_INVALID_RESPONSE`.
- Thought parts are skipped; non-text parts are rejected. Returns an immutable `List<String>`; count, blank, length and duplicate rules are left to Person 2's service.
- No retries, fallbacks or logging of keys, drafts or provider bodies.

## Tests run

From `backend/`:

```sh
./mvnw -q -Dtest=GeminiRewriteClientTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result: 56 tests, 0 failures. All use an ephemeral loopback server; no real key.

Note: the connection-failure test uses a server that accepts and then drops the connection. A closed port is not used because Windows retries refused connections until the connect timeout, which would wrongly look like `AI_TIMEOUT`.

## Live provider called?

**Yes, synthetic drafts only**, on 2026-10-10, with a key loaded from a local ignored `.env`.

- Model `gemini-3.5-flash-lite`, `generateContent`, free tier: accepted, including the structured schema and `thinkingLevel: MINIMAL`.
- 18 live requests in total (1 probe, the 16-case matrix, 1 instruction-like draft): all HTTP 200 with three valid rewrites, each in 0.7–1.2s, well inside the 20s deadline.
- No quota errors were hit. The project's exact free-tier limits were not read from AI Studio.
- The server log contained no key and no draft text.

If the model or the thinking option is ever rejected (the request would fail as `AI_UNAVAILABLE`), update `gemini.md`, the `GEMINI_MODEL` default and the tests together.

## Known limitations

- The refusal finish-reason set (`SAFETY`, `RECITATION`, `BLOCKLIST`, `PROHIBITED_CONTENT`, `SPII`, `IMAGE_SAFETY`) is based on documented values but has not been checked against live responses.
- Prompt quality is unverified until the live matrix runs.

Shared changes requiring coordination: none.

Ready for: isolated review — yes; integrated tests — yes (passing); live demo — after the live probe.
