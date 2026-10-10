# Person 2 handoff — rewrite service complete

**Status:** Person 2 implementation and verification are complete against Person 1's committed shared types. The combined Person 1/2 backend passes its tests on Java 21. Live Gemini generation remains Person 3's work; final live frontend integration remains Person 4's work.

## Baseline and contract

- Responsibility: rewrite orchestration and input/output validation.
- Contract: `docs/backend/contract.md` v1.0, unchanged.
- Foundation/base: `a4b48f7` (`backend p1 done`) on `main`, containing the shared contracts, configuration and API.
- Implementation commit: none; changes remain uncommitted. No push performed.

## Files changed

- `backend/src/main/java/com/sayless/service/DefaultRewriteService.java`
- `backend/src/test/java/com/sayless/service/DefaultRewriteServiceTest.java`
- `docs/backend/handoff-person-2.md`

The earlier temporary drafts are installed in their assigned repository paths and verified against the actual foundation. No shared interfaces, configuration, controller, provider, frontend or existing tests were modified.

## Interface and completed behavior

`DefaultRewriteService` is the single stateless `@Service` implementing the existing `RewriteService`. Its constructor takes the existing `AiRewriteClient`. It imports Person 1's `Tone`, `ErrorCode`, `RewriteException`, and `RewriteLimits` without copying or changing them.

- Rejects null messages, strips outer Java whitespace, and checks blank/2,000 UTF-16-unit message limits before tone parsing.
- Uses exact shared `Tone.fromWireValue` parsing; no default, trimming, or case folding of the supplied tone.
- Calls the provider exactly once after valid input with the normalized message and typed tone.
- Requires exactly three non-null/nonblank rewrites, each at most 4,000 UTF-16 units after outer stripping.
- Rejects duplicates using the specified Unicode-whitespace collapsing and `Locale.ROOT` comparison. Returned text preserves internal whitespace, line breaks, case, punctuation and literal markup.
- Returns an immutable defensive list in provider order; state is request-local and independent across concurrent calls.
- Propagates intentional provider exceptions unchanged. Unexpected programming errors remain unexpected errors for the API boundary to handle.
- Adds no retries, persistence, cache, content logging or fixture fallback.

## Verification performed

Focused command, from `backend/`:

```sh
sh ./mvnw -q -Dtest=DefaultRewriteServiceTest test
```

Result: **58 tests passed, 0 failures/errors/skips**. This initial focused run used default JDK 25.0.2, compiling for the project's Java 21 target.

The full backend suite passed on both the default runtime and the installed Java 21 runtime. Required-runtime command, from `backend/`:

```sh
env JAVA_HOME=/Users/ayanatif/Library/Java/JavaVirtualMachines/temurin-21.0.11/Contents/Home sh ./mvnw -q clean verify
```

Use your own installed JDK 21 path on another machine. The wrapper lacks an executable bit in this checkout, so `sh ./mvnw` was used without modifying Person 1's wrapper file.

Java 21.0.11 result: **133 tests passed, 0 failures/errors/skips**:

| Suite | Passing tests |
|---|---:|
| `DefaultRewriteServiceTest` | 58 |
| `RewriteControllerTest` | 37 |
| `AiConfigurationTest` | 19 |
| `ContractTypesTest` | 11 |
| `WebConfigurationTest` | 7 |
| `SaylessBackendApplicationTests` | 1 |

The previously missing service bean now resolves, and the existing full application context-load test passes without changing that test. Service tests use an in-memory recording client; no real Gemini key or live Gemini request was used.

Coverage includes all four tones, null/blank/unsupported/padded input, exact ASCII and supplementary-character length boundaries, invalid result lists, duplicate comparisons including Unicode whitespace and Turkish locale, exact presentation text, immutable copies, consecutive/concurrent request isolation, provider error propagation, unexpected bugs, and recovery after failure.

## Remaining team integration work

- Person 3 supplies the enabled `GeminiRewriteClient` bean and real generation using the existing `AiRewriteClient` interface. Person 2's service requires no changes to adopt it.
- Person 4 runs real controller/service/provider flow tests and live tone/intent checks, connects the frontend and completes release gates.
- Person 1's tracked `backend/target/` cleanup remains outstanding in this baseline. Generated verification output is not part of this source contribution.
- Structural validation cannot establish semantic quality; no claim of live AI or completed live MVP verification is made.

No unresolved Person 2 contract question or shared-interface change remains. Ready for scoped review and integration with Persons 3 and 4.
