# Backend decisions and handoff record

Maintainer: **Person 4**, coordinating with each owner. Keep this file factual; a planned decision is not a completed verification. Never put credentials or personal drafts here.

## Baseline and gates

| Item | Value/status at plan creation |
|---|---|
| Inspected baseline | `97f1ca7`, branch `main`, October 10, 2026 |
| Contract version | 1.0 |
| Shared foundation revision | Pending — Person 1 supplies; Person 4 records |
| Integration branch/revision | Pending — Person 4 records the team-selected branch |
| Java build target | 21; local executable observed as 25, so verify team toolchains |
| Spring Boot / wrapper Maven | Existing POM 4.1.1 / wrapper distribution 3.10.0; clean foundation gate pending |
| Provider choice | Gemini free API, explicitly selected by user |
| Planned model | `gemini-3.5-flash-lite`; documentation checked, actual project access pending |
| API family | Non-streaming `generateContent` |
| Transport/library | JDK HTTP client + Boot-managed JSON; no provider SDK |
| Final frontend serving | Same-origin packaged Spring Boot resources |
| Default no-key behavior | Provider disabled; boot succeeds, rewrite returns 503 |
| Automated tests | Synthetic/local fixtures; no live key or provider quota |
| Live probe / quality matrix | Pending, owned by Person 3 / Person 4 respectively |
| Deployment/public hosting | Out of scope for this implementation plan |

## Decisions that must not diverge across branches

1. Public request stays `{message, tone}` and success stays `{rewrites: [string, string, string]}`.
2. One shared `contract` package, one controller, one rewrite service, and exactly one conditional provider bean.
3. Input/output limits, exact tone parsing, duplicate comparison and error codes are defined only in `contract.md` and implemented from shared constants/types.
4. Person 1 edits shared POM/config; Person 3 edits provider/prompt; Person 2 edits service; Person 4 edits integration/frontend wiring. Shared changes go through the owner.
5. One model/schema specification; model changes require documented free-tier availability and compatible request configuration.
6. No automatic retries, model/provider fallback, mock fallback, or paid-tier activation.
7. User messages/results are never persisted by SayLess; frontend privacy language acknowledges Gemini free-tier processing.
8. Java 21 / existing Boot version stays fixed unless foundation verification proves an issue and the team records a replacement together.

## Open checks with owners

| Check | Owner | Blocks | Evidence required |
|---|---|---|---|
| Clean wrapper build and compatible Java 21 runtime | 1 | Foundation release | Actual command/output summary |
| Generated-file index cleanup | 1 | Clean shared merge | `git ls-files backend/target` empty after intended cleanup |
| Free project/key available privately | 3 | Live provider probe only | Confirmed configured credential; never the value |
| Model access/structured schema/thinking option | 3 | Live integration | Successful synthetic probe with elapsed time |
| Actual free-tier quota and model availability | 3 | Scheduling live matrix | Observed limits/date from AI Studio, no guessed numeric quota |
| Foundation and feature revisions integrated | 4 | Combined test gate | Recorded SHAs and scoped diff review |
| CORS/local port conflicts on team machines | 4 with 1 | Separate-server checks | Actual ports/origins used, matching configuration |
| Browser/clipboard and live quality results | 4 | Complete-demo claim | Test results separated by evidence level |

Owners can continue independent fixture-based work while account or live quota checks are pending. Time elapsed is not evidence that a check passed.

## Change protocol and record template

For a shared change, describe it here via the document owner before other agents implement against it. Include all affected consumers. Person 1 updates shared contract/types, Person 3 updates provider specification if relevant, and Person 4 records incorporation. A local preference is not sufficient reason to rename a common field.

```text
Decision ID/date:
Problem demonstrated:
Old behavior/signature/property:
Agreed replacement:
Contract version after change:
Owner and affected people:
Files/tests/docs to update:
Foundation/change commit:
Person 1 incorporated:
Person 2 incorporated:
Person 3 incorporated:
Person 4 incorporated:
Verification evidence:
```

## Handoff template

Each owner creates `docs/backend/handoff-person-N.md` when implementing. Those files are intentionally not pre-filled with success claims.

```text
Person / responsibility:
Contract version and foundation SHA used:
Branch / implementation commit:
Files changed:
Public interface/config consumed or provided:
Behavior completed:
Tests run (exact command and result):
Tests not run, with reason:
Live provider called? If yes, synthetic only; model/date/result:
Known limitations or failures:
Dependencies needed from another person:
Shared changes requiring coordination:
Ready for isolated review / integrated tests / live demo:
```

## Agent startup prompt for the coordinator

> Read `docs/backend-plan.md` and every linked contract/person brief before integrating. Inspect the current branch and diffs; do not assume the plan's baseline is still current. Check that all branches use the recorded foundation and current contract. Merge or propose changes through the team's authorized Git workflow in the documented order. Route defects to the owning person instead of replacing their work. Run the combined tests, packaged browser checks and permitted live checks, then update this record and the runbook with actual evidence and pending items.
