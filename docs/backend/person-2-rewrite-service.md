# Person 2 — rewrite orchestration and validation

## Give your agent this task

> Implement Person 2's work after reading `docs/backend-plan.md`, `docs/backend/contract.md`, this brief, the current shared types, and any applicable repository instructions. Own the service package and its tests only. Use the foundation's `RewriteService`, `AiRewriteClient`, `Tone`, `RewriteException`, `ErrorCode`, and `RewriteLimits`. Complete isolated tests without live Gemini. Write `docs/backend/handoff-person-2.md`. Do not re-create shared DTOs, implement HTTP/controller/provider code, or change frontend behavior.

## Ownership and dependencies

Own `backend/src/main/java/com/sayless/service/**` and `backend/src/test/java/com/sayless/service/**`. Wait for the Person 1 foundation commit before importing shared types. While waiting, prepare the test matrix; do not create another `AiRewriteClient` interface in your package.

Implement `DefaultRewriteService implements RewriteService` as one stateless `@Service` with constructor-injected `AiRewriteClient`. Use optional private helpers under your own package. It requires no provider key, HTTP library, controller DTO, database or request history.

## Processing sequence

1. Reject a null message. Apply `strip()`, then reject blank or normalized length above `RewriteLimits.MAX_MESSAGE_LENGTH`. Length is Java UTF-16 `String.length()`, not bytes or Unicode code-point count.
2. Parse tone using shared `Tone.fromWireValue` with exact lowercase matching. Do not trim or lowercase tone. Validate message before tone so failure ordering stays deterministic.
3. Call `AiRewriteClient.generate(normalizedMessage, tone)` **exactly once** after valid input. No fallback, retry, cache, fixed sleep, or asynchronous wrapper around the client.
4. Validate the returned list using contract section 3: exactly three strings, nonblank after strip, max length, distinct comparison keys. The provider parser rejects non-string JSON values; service still rejects null list/elements and invalid string content.
5. Return a defensive immutable list of normalized strings in provider order. Preserve interior whitespace/newlines, case, punctuation and HTML-like text exactly.
6. Pass intentional `RewriteException` provider failures through unchanged so Person 1 maps them. Do not turn all exceptions into `AI_INVALID_RESPONSE`; unexpected bugs remain errors handled by the API boundary.

There are two intentionally different boundaries: Person 3 validates provider envelopes/JSON types; you enforce business output validity. A provider test double can therefore supply a bad list and the service must still reject it. Structural distinctness is a minimum requirement; semantic quality is checked by humans in the live matrix.

## Required tests

Use `service/DefaultRewriteServiceTest.java`, JUnit and a recording fake client or a mock. No Spring context is needed for these tests. Use shared constants rather than copying numeric limits into production logic; boundary test inputs should independently assert the documented values.

| Group | Cases and expectations |
|---|---|
| Valid input | All four tones; surrounding whitespace removed; multiline, quotes, backslashes, HTML-like strings, and emoji preserved inside the draft |
| Message validation | null, empty, spaces, tabs/newlines, exact length 2,000, length 2,001, and supplementary-character boundary; invalid cases never call provider |
| Tone validation | null, empty, unknown, `Professional`, ` professional `; reject rather than default; provider never called |
| Request ordering | Both invalid message and tone still fail before provider; a valid request calls provider once with typed tone |
| Result shape | null list, zero/two/four elements, null element, blank element all become `AI_INVALID_RESPONSE` |
| Result boundaries | Normalized output length 4,000 accepted, 4,001 rejected; do not truncate |
| Distinctness | Exact duplicates and differences only in case/whitespace fail; genuinely different wording passes |
| Presentation text | Strip outer whitespace only; preserve internal newlines, spaces and literal markup |
| Isolation | Returned list is immutable and unaffected by mutation of a fake client's source list; consecutive requests cannot leak previous results |
| Error propagation | Provider `AI_TIMEOUT`, `AI_RATE_LIMITED`, `AI_REFUSED`, `AI_UNAVAILABLE` and disabled error remain the same codes |

Command from `backend/`:

```sh
./mvnw -Dtest=DefaultRewriteServiceTest test
```

Merge the service into integration after the foundation. The disabled client allows no-key application wiring once the API branch also arrives. Do not alter the existing context test to hide failures; Person 4 owns full wiring verification.

## Done and handoff

Done means the isolated test matrix passes, one service bean exists, provider is called only after validation, and all output rules are enforced. Record your commit/base, exact test command/result, the injected interface and any unresolved contract question in `handoff-person-2.md`. Ask the relevant owner to fix defects outside your assigned files rather than making parallel implementations.
