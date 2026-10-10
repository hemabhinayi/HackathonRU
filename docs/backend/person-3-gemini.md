# Person 3 — Gemini client and generation quality

## Give your agent this task

> Implement Person 3's Gemini backend work using `docs/backend-plan.md`, `docs/backend/contract.md`, `docs/backend/gemini.md`, this brief, the current foundation types, and any applicable repository instructions. Use the free Gemini Developer API and the specified generateContent wire contract. Work in your assigned provider/prompt/test paths. Tests must use local fixtures by default and never require a real key. Preserve the shared interface and write `docs/backend/handoff-person-3.md`. Do not implement the controller/service, add an alternate provider, change shared configuration unilaterally, or enable paid billing.

## Ownership

- Production: `backend/src/main/java/com/sayless/provider/**`, including `GeminiRewriteClient` and any internal request builder/parser.
- Prompt: `backend/src/main/resources/prompts/rewrite-system.txt`.
- Tests: `backend/src/test/java/com/sayless/provider/**` and `backend/src/test/resources/gemini/**`.
- Provider specification and your own handoff. Person 1 owns all common configuration/POM edits; submit specific needs to that owner.

## Implementation steps

1. Read the committed `AiRewriteClient` and `AiProperties` from the foundation. Confirm there is no existing provider implementation before creating one. No credential belongs in source or a fixture.
2. Verify the current official docs linked in [gemini.md](gemini.md). Confirm the selected model is offered to the team's free project. Lack of account/key access blocks the **live probe**, not local implementation/tests. Record the missing dependency honestly.
3. Implement one `GeminiRewriteClient implements AiRewriteClient`, registered only when `sayless.ai.enabled=true`. Person 1's disabled bean covers false/missing mode. No automatic `@Primary` workaround for duplicate beans.
4. Read the prompt resource once, construct the specified one-candidate structured-output request with safe JSON serialization, and send it through the configured JDK HTTP client. Use the key header, never a URL parameter. Do not use the Interactions API or a chat-completions-shaped payload.
5. Apply a full-response deadline and cancellation. The application should return before the frontend's 30-second abort. Do not mistake a connect timeout for an overall response deadline. Handle interruption without swallowing the interrupt flag.
6. Implement typed envelope parsing, finish/block handling and the shared error mapping. Parse model text as JSON and reject malformed output. Do not attempt to clean up Markdown or manufacture missing options.
7. Keep provider wire types internal. Return only `List<String>` and throw only shared intentional errors for expected provider failures. Let unrelated coding defects surface for debugging through a safe application boundary.
8. Test using a loopback server, including delayed response-body completion. Only after tests pass perform the single coordinated live probe with synthetic content. Record the exact model, latency, structured-output compatibility and free-tier availability.

## Test fixtures and cases

Use small synthetic envelopes in `src/test/resources/gemini/`. Suggested files: `success.json`, `blocked-prompt.json`, `blocked-candidate.json`, `truncated.json`, `malformed-model-json.json`, `wrong-result-types.json`. Use no captured personal draft or real key. These are Gemini-shaped fixtures; the shared frontend fixture is not a substitute for the outer envelope.

Suggested tests: `GeminiRewriteClientTest` for transport and mapping; `GeminiResponseParserTest` if parsing is extracted. Use an ephemeral port to avoid collisions and stop the fixture server after each suite.

| Case | Expected evidence |
|---|---|
| Request construction | Correct path/model/method, JSON content type, key header, no URL credential, one candidate, correct schema, four tone values and prompt |
| Escaping | Quotes, backslashes, newline, Unicode and instruction-like message text survive JSON serialization as user content |
| Success | Outer envelope parsed; model-text JSON produces the three strings in order; no provider envelope or thought text returned |
| Split parts | Text parts concatenate correctly; thought-marked parts excluded; unexpected generated non-text content rejected |
| Provider 429 | `AI_RATE_LIMITED`; exactly one outbound request |
| Provider auth/not-found/5xx | `AI_UNAVAILABLE`; no response body/credential leaks |
| Deadline | Slow headers and stalled body both end with `AI_TIMEOUT`; request canceled; test duration bounded |
| Transport failure | Connection failure maps safely, no retry; subsequent valid call succeeds |
| Block/refusal | Prompt-level and candidate-level provider signals map to `AI_REFUSED` |
| Bad envelope | Missing/extra candidate, empty text, malformed outer JSON, unexpected finish reason or truncation fail safely |
| Bad model JSON | Markdown-wrapped JSON, non-object JSON, missing rewrites, non-string entries or unexpected top-level shape → `AI_INVALID_RESPONSE` |
| Secret handling | Neither error messages nor captured logs contain fake key, supplied draft or raw provider error content |

Person 2 owns duplicate/blank/length enforcement on the extracted list. Include an integrated fixture with duplicates later so Person 4 proves that the service rejects a parser-success case.

From `backend/`, for the suggested test names:

```sh
./mvnw -Dtest=GeminiRewriteClientTest,GeminiResponseParserTest test
```

Use the actual names if you keep parsing inside the client. No real provider requests in default Maven tests or CI.

## Free-tier coordination and handoff

One person runs the initial provider probe: you. Then Person 4 owns the live demo matrix. Multiple keys from one project do not provide separate quota pools. Do not repeat full live suites on every commit, turn quota errors into mock success, or print keys during troubleshooting.

Handoff includes actual configured model, documentation check date, safe property names, provider-test result, whether the single live probe passed, and any quota/account restriction. If a new model is needed, coordinate the spec/config/test update before other agents depend on it. Never report model quality as guaranteed merely because structured JSON parsed.
