# Person 3 handoff — SayLess

> Historical frontend handoff. Current backend ownership and planned Gemini integration are defined in [the four-person backend plan](backend-plan.md).

The input page and submit handler are complete. `frontend/index.html` uses Person 4's stylesheet and `frontend/script.js` imports the output module directly.

- Title, tagline, labeled message field, four keyboard-accessible tone buttons, submit button, privacy note, and results container are present.
- Professional is selected by default. Tone values sent to the API are lowercase.
- Empty and whitespace-only messages show a friendly error and focus the message field without making a request.
- Submission shows loading, disables submit/tone buttons, and guards repeated form submissions. Completion restores controls. Errors preserve the draft and tone.
- API requests send JSON `{ message, tone }` via POST, trim outer message whitespace, check HTTP success, and time out after 30 seconds. Error responses are not displayed verbatim. Invalid results use the output module's validation.
- Messages and results are not persisted.

## Run locally

From the repository root:

```sh
python3 -m http.server 5500 --bind 127.0.0.1 --directory frontend
```

Open `http://127.0.0.1:5500/`.

The checked-in page explicitly uses **mock mode** because the backend currently has no rewrite endpoint. A visible notice explains that the three PRD sample rewrites are fixed and arrive after one second; neither the message nor tone affects them. This verifies the frontend flow, not AI generation.

## Connect the backend

In `frontend/index.html`, change the `rewrite-mode` meta value from `mock` to `api`. The request handler then posts to `/api/rewrite` on the page's origin. There is no automatic mock fallback after an API failure.

If frontend and backend use different origins, set the `rewrite-endpoint` meta value to the confirmed absolute endpoint URL (for example, `http://localhost:8080/api/rewrite` for local development). Persons 1 and 2 must enable CORS for the frontend origin, including the JSON POST preflight. No provider key belongs in frontend code.

The backend must return exactly three nonblank strings in `{ "rewrites": [...] }`. Real backend availability, CORS and AI rewrite quality still need joint verification once the endpoint exists.

## Verification

Headless Chrome checks passed on October 10, 2026:

- Blank and whitespace-only messages show the validation error; all four tone buttons maintain one selected state.
- The explicit mock flow transitions through loading to three cards.
- Intercepted API requests send the expected trimmed message and lowercase tone for each of the four choices. Repeated form submissions produce one request.
- HTTP 503, network failures, and malformed JSON display safe errors, preserve the draft/tone, restore controls, and allow successful retry.
- The page has no horizontal overflow at 320, 375, 768 and 1280px. Desktop and mobile screenshots were inspected.
- All 17 existing Person 4 output checks pass, with no uncaught JavaScript errors in the exercised flows.

API checks used intercepted responses, not a live backend. The 30-second timeout is implemented but was not exercised in these checks.

No commit or push was made; the preexisting Person 4 and backend working changes remain in the checkout.
