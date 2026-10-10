# Person 4 Tasks — SayLess Output UI and Styling

> This brief describes the completed frontend phase. The current Person 4 backend/integration assignment is in [the four-person backend plan](backend-plan.md) and [Person 4's new brief](backend/person-4-integration.md).

## 1. Instructions for the AI implementing this document

Implement Person 4's contribution to SayLess: the output display, copy interactions, loading and error states, responsive styling, and the checks needed to demo those features. Follow the requirements below, using plain HTML, CSS, and JavaScript.

First read `SayLess_PRD.md`, `docs/frontend-plan.md`, and the current files in `frontend/`. Read any applicable `AGENTS.md` instructions. Inspect the files again at implementation time because teammates may have added code since this document was written.

Complete the implementation and verify it. Preserve teammates' existing work, use the agreed API contract, and give a short handoff explaining the files changed, how Person 3 calls the functions, what was checked, and anything still awaiting backend integration.

This document is an implementation brief. Creating this document does not itself implement the frontend.

## 2. Product context and required outcome

SayLess helps a user rewrite a draft message in a chosen tone. The user enters text, selects Professional, Casual, Diplomatic, or Funny, clicks **Rewrite my message**, and receives three rewritten options to copy.

Person 4's work is complete when:

- Three returned rewrites appear as three readable result cards.
- Every card has its own working copy button with brief **Copied!** feedback.
- The output area clearly shows a pending request and an unsuccessful request.
- The page and result cards work on phones and laptops.
- Person 3 can call the three agreed output functions without managing the card markup.
- These features work with mock data and with the real backend response.

The frontend must not store messages or results. The backend owns AI generation; Person 4 presents the text it returns.

## 3. Ownership and files

The frontend files were empty when this brief was prepared. Do not assume they are still empty when implementing it.

| File or feature | Owner | Person 4's work |
|---|---|---|
| `frontend/index.html` | Person 3 | Coordinate the output container and script loading; preserve the input form and privacy note. |
| `frontend/script.js` | Person 3 | Provide the output function interface and integration example; preserve input, tone selection, validation, and request logic. |
| `frontend/output.js` | Person 4 | Create the output module containing result rendering, copy behavior, loading, and errors. |
| `frontend/styles.css` | Person 4 | Implement the overall visual styling and responsive layout, including Person 3's input controls. |
| `docs/person 4 tasks.md` | Implementation brief | Use this as the checklist for Person 4's contribution. |
| `backend/` | Persons 1 and 2 | Coordinate the endpoint and errors; do not implement or modify backend features as part of Person 4's work. |

Do not replace the whole `script.js` or `index.html` to implement the output side. If an integration edit is needed, make it small and coordinate it with Person 3. Avoid competing implementations of the submit handler.

Do not add accounts, a database, history, favorites, document uploads, multiple languages, or a chat assistant. Avoid adding a frontend framework or build system for this work.

## 4. Agree on the integration contract first

### Output container

Person 3 provides one container below the input form:

```html
<div id="results"></div>
```

Person 4 owns the contents of that container. The output module creates the loading text, error display, result heading, and cards inside it. Do not require Person 3 to generate individual cards or add separate loading/error elements elsewhere.

Agree with Person 3 on the existing input/control selectors before writing their styles. Preserve the IDs that Person 3 uses in JavaScript. Prefer reusable classes for styling and keep output-specific classes scoped to the output area.

### Three public functions

Create `frontend/output.js` with these named exports:

```js
export function renderResults(rewrites) { /* display three results */ }
export function showLoading() { /* display pending state */ }
export function showError(msg) { /* display a request/validation error */ }
```

- `renderResults(rewrites)` receives the array of strings, not the whole response object.
- `showLoading()` takes no arguments and immediately replaces the previous output state with a loading state.
- `showError(msg)` receives a user-facing message and replaces the previous output state with an error state.
- These functions handle their own output cleanup. Person 3 does not need a fourth function to hide loading or clear old results.
- Keep helper functions private unless an additional export is actually needed.
- Initialize the output DOM after `#results` exists. Avoid attaching duplicate listeners during initialization or rerendering.

For the empty frontend scaffold, use ES modules. Person 3 imports the functions into `script.js`:

```js
import { renderResults, showLoading, showError } from './output.js';
```

The HTML then loads `script.js` as a module:

```html
<script type="module" src="./script.js"></script>
```

Person 3 owns that script tag and import integration. An imported `output.js` does not need its own script tag. If Person 3 has already established classic scripts, agree on one compatible approach instead of mixing module exports with classic loading. Document the actual interface used.

## 5. Implement the output state transitions

Use one clear output state at a time:

| Trigger | Required output state |
|---|---|
| Page first opens | Empty output area or a short, unobtrusive instruction; no fake results or loading indicator. |
| Person 3 calls `showLoading()` | Show **Rewriting...**; remove previous results and request errors. |
| Person 3 calls `renderResults(validRewrites)` | Stop loading; remove the previous error; show exactly three cards. |
| Person 3 calls `showError(message)` | Stop loading; remove old cards; show the supplied error clearly. |
| Another rewrite starts | Replace the previous output with loading again. |
| Copy succeeds | Keep all cards; update only the clicked card's copy feedback. |
| Copy fails | Keep all cards; show a useful message on the affected card. |

Set `aria-busy="true"` on the output container during loading and set it back to `"false"` for results and errors. Ensure the text **Rewriting...** is available to assistive technology through a status/live region. If using a spinner, treat its graphic as decorative; keep the text as well.

`renderResults()` and `showError()` must fully end the loading presentation. Do not leave a spinner alongside completed results or an error.

Person 3 owns disabling the rewrite button while the request is pending and reenabling it in `finally`. Include that expectation in the handoff so repeated submissions do not accidentally create overlapping requests. If their handler permits overlapping requests, Person 3 must ensure an older response cannot replace a newer one.

## 6. Implement `renderResults(rewrites)`

### Validate the response shape

Before creating cards, check that:

- The argument is an array.
- It contains exactly three entries.
- Every entry is a string containing non-whitespace text.

For an invalid argument, call `showError()` with a friendly message such as **We couldn't load the rewrite options. Please try again.** Do not show `undefined`, `[object Object]`, an incomplete result set, or a JavaScript exception to the user.

Do not generate missing rewrites, silently substitute mock data, or alter the backend's returned meaning. The backend owns producing three distinct options that preserve intent and match the chosen tone. Report obviously duplicated or unsuitable real results to Persons 1 and 2 during demo testing.

### Create the result cards

For a valid array:

1. Clear the previous output state and stop loading.
2. Add a readable result heading, such as **Your rewrites**.
3. Create exactly three cards in response order, labeled **Option 1**, **Option 2**, and **Option 3**.
4. Put each rewrite's full text in its card.
5. Add a **Copy** button to each card and a place for that card's copy status.
6. Attach each button to its own rewrite string.

Use DOM creation and `textContent` for returned rewrite text and error messages. Treat both as text, not HTML. A rewrite containing `<script>` or HTML-like text must appear literally and must not execute or create markup.

Keep the complete rewrite selectable and visible. Do not truncate it, shorten it, or impose a fixed height that hides content. Preserve intentional line breaks where practical, using a style such as `white-space: pre-wrap`. Use wrapping such as `overflow-wrap: anywhere` so a long URL or word cannot push the page wider than the screen.

Calling `renderResults()` again must replace the old cards rather than append three more. Avoid retaining listeners or timers that act on cards removed by a later request.

## 7. Implement copy buttons and feedback

For each card:

1. Use a native `<button type="button">` so the button is keyboard accessible and cannot submit the input form.
2. Give it a clear accessible name, such as **Copy option 1**.
3. On a click, call `navigator.clipboard.writeText(rewriteText)` from that user interaction.
4. Await the returned promise. Only after success, change that button's visible label to **Copied!**.
5. Restore the label to **Copy** after about 1.5 seconds.
6. Provide accessible success feedback through the card's status/live region.

Copy the exact rewrite string from that card, including punctuation and line breaks. Do not include the option heading, button label, or another card's text. Checking for non-whitespace content must not cause copy to trim or rewrite the displayed string.

Handle rapid repeated clicks predictably: reset or clear the previous feedback timeout for that button. Keep feedback independent between cards. If a card is removed while an asynchronous copy or timeout is pending, avoid updating the new output state with stale feedback.

Handle missing clipboard support and rejected clipboard permission without an unhandled promise rejection. Keep the card and text visible and show a message such as **Couldn't copy automatically. Select the text and copy it manually.** Never show **Copied!** after failure.

The Clipboard API works in supported secure contexts, including HTTPS and localhost. Serve the frontend locally for development; do not rely on opening the HTML through `file://`. A manual selection-and-copy message is sufficient as the failure fallback; a legacy clipboard library is not required.

## 8. Implement `showLoading()` and `showError(msg)`

### `showLoading()`

- Replace any previous cards or request error with the loading presentation.
- Display **Rewriting...** immediately after the request starts.
- Use a small spinner only if useful; text alone satisfies the MVP.
- Keep the state readable and announced without moving keyboard focus unnecessarily.
- Keep the page layout usable while waiting. Avoid an overlay that blocks the whole page.

### `showError(msg)`

- Replace loading and old cards with one clear error message near the output area.
- Display a nonempty string supplied by Person 3 as plain text.
- If the argument is missing, blank, or not a string, use a fallback such as **Something went wrong. Please try again.**
- Use an alert/live-region pattern appropriate for announcing a new error.
- Distinguish the error using text and styling; do not rely on red color alone.
- Do not clear the user's input message or selected tone; Person 3 owns those controls.
- Allow a later `showLoading()` or `renderResults()` call to replace the error normally.

Person 3 maps empty input, HTTP failures, invalid JSON, network failures, and backend error responses to suitable user-facing messages before calling this function. Person 4 supplies the display and fallback behavior. Do not show raw stack traces, HTML error pages, or credentials.

Copy errors belong within the affected card. Do not call the global `showError()` for a clipboard failure, because doing so would discard all results.

## 9. Implement the overall look in `styles.css`

Style the complete existing page, including Person 3's markup. Aim for a clean, readable interface that can be completed within the hackathon window.

Cover these elements:

- Page background, typography, readable line height, and a centered content container.
- SayLess title and tagline.
- Message label and textarea, with sufficient height and comfortable padding.
- Four tone controls, including a clear selected state using Person 3's agreed class or attribute.
- The primary **Rewrite my message** button, including hover, focus, and disabled styling.
- Privacy note supplied by Person 3.
- Result heading, three cards, option labels, rewrite text, and copy buttons.
- Loading/status text and request and copy error messages.

Use consistent spacing, borders, colors, and button sizing. CSS custom properties for repeated values are useful. Avoid unnecessary dependencies and decorative features that delay a working rewrite flow.

### Responsive behavior

- Start with a single-column layout suitable for a narrow phone.
- Use fluid widths and `box-sizing: border-box`; avoid fixed widths that exceed the viewport.
- Let tone controls wrap when necessary.
- Keep buttons large enough for comfortable tapping; aim for roughly 44px height.
- At laptop widths, use available space for a readable form and results. Cards may stay stacked or use a grid if their text remains readable.
- Ensure all three cards and copy buttons remain reachable by scrolling.
- Test at approximately 320px, 375px, 768px, and 1280px viewport widths.
- Long messages, multiline text, and long unbroken strings must not cause horizontal scrolling.

### Accessibility and visual states

- Maintain readable text contrast and a visible keyboard focus outline.
- Style selected tone buttons consistently with the selection logic supplied by Person 3.
- Preserve native keyboard behavior and use real buttons for copy.
- Do not hide the privacy note on smaller screens.
- If adding animation, respect `prefers-reduced-motion`.
- Avoid turning ordinary page text into an image or adding icons without meaningful labels.

## 10. Use the agreed mock response before the backend is ready

Use this exact fixture from the PRD:

```js
const MOCK_RESPONSE = {
  rewrites: [
    'Could you let me know when you expect to finish your part?',
    'I wanted to check in on your section so we can stay on schedule.',
    'Can you share an update on your part of the project?'
  ]
};
```

The mock request must resolve after approximately 1 second so loading is visible. The simplest helper is:

```js
function mockRewrite() {
  return new Promise((resolve) => {
    setTimeout(() => resolve(MOCK_RESPONSE), 1000);
  });
}
```

Person 3 owns adding the mock request to their submit handler. Give them this expected sequence:

```js
// Inside Person 3's handler, after input validation:
showLoading();
try {
  const data = await mockRewrite();
  renderResults(data.rewrites);
} catch {
  showError('We could not rewrite your message. Please try again.');
} finally {
  // Person 3 reenables the rewrite button here.
}
```

This is a handoff example, not a replacement for Person 3's complete handler. They also disable the button before the asynchronous request starts.

For Person 4's independent development, a small separate development HTML page may load `output.js`, provide `#results`, and exercise the mock, loading, and errors. Keep it clearly separate from the production page and do not build a second input flow. Alternatively, use Person 3's working page as soon as it is available.

The sample rewrites are fixed test data. They do not prove that arbitrary messages or tones are being processed. Keep mock mode explicit and easy to replace, and never silently fall back to it when a real request fails.

## 11. Coordinate real backend integration

Prepare these questions for Persons 1 and 2 and include them in the handoff; contact teammates only through an authorized team workflow:

- What is the confirmed backend base URL? `http://localhost:8080` is an assumption until confirmed.
- Is `POST /api/rewrite` ready, and does it accept the request and return the response below?
- If the frontend and backend have different origins, is CORS enabled for the actual frontend origin, including the JSON request's preflight?
- What is the agreed error-response format and which message is safe to display?

Request:

```json
{
  "message": "I need my teammate to finish their part",
  "tone": "professional"
}
```

Response:

```json
{
  "rewrites": [
    "Could you let me know when you expect to finish your part?",
    "I wanted to check in on your section so we can stay on schedule.",
    "Can you share an update on your part of the project?"
  ]
}
```

Person 3 switches the mock helper to a real `fetch` call using `POST` and `Content-Type: application/json`. Tone values are lowercase: `professional`, `casual`, `diplomatic`, or `funny`. Use `/api/rewrite` for the same origin or the confirmed base URL for a separate backend origin.

The output sequence stays the same: `showLoading()`, then `renderResults(data.rewrites)` on success or `showError(message)` on failure. Person 3 must check HTTP status and handle JSON/network failures; a fulfilled `fetch` promise alone does not mean the request succeeded.

Person 4 verifies that live response text appears correctly and copies correctly. Persons 1 and 2 own CORS and the AI API connection. Do not add `mode: 'no-cors'` as a workaround: it prevents JavaScript from reading the response needed by this UI.

Do not claim that live AI integration passed when only a mock or stub endpoint was available. Record that limitation in the handoff.

## 12. Preserve the PRD's privacy requirements

- Keep messages and results only in the current page's memory and DOM.
- Do not save them to localStorage, sessionStorage, cookies, IndexedDB, a database, or a history feature.
- Do not add analytics that capture drafts or results, and avoid logging message content.
- Preserve Person 3's note that messages are sent to an AI service and users should avoid sensitive personal information.
- Keep AI API keys and AI-provider calls on the backend. Person 4's code needs neither a key nor a direct AI request.
- Clipboard writing happens only when the user clicks a copy button. Do not read the clipboard.

## 13. Verify Person 4's work

Use focused browser checks or the repository's existing test tools. A new test framework is not necessary for this small frontend. If the shared input flow is not ready, exercise the output functions in the separate development page.

| Check | Expected result |
|---|---|
| Initial page | No fabricated results or active spinner; existing input UI remains usable. |
| One-second mock request | **Rewriting...** appears, then exactly three cards show the fixture in order. |
| Each copy button | Pasting elsewhere gives that card's exact text; only its button briefly shows **Copied!**. |
| Rapid copy clicks | Feedback resets predictably without affecting other cards. |
| Clipboard unavailable or rejected | Useful inline guidance appears; results remain visible; no false success or uncaught error. |
| Request error | Loading stops; error is visible; old cards are removed. |
| Retry after error | Loading and then results work normally; old error disappears. |
| Second successful request | Old cards are replaced; there are still exactly three. |
| Invalid result data | Null, missing arrays, wrong entry counts, non-string entries, and blank strings show a friendly error. |
| Literal HTML-like text | Text such as `<b>hello</b>` is displayed literally; no markup is executed. |
| Long and multiline rewrites | Full text remains readable and selectable; no clipping or horizontal overflow. |
| Phone and laptop widths | Form, tone controls, results, and buttons fit and remain usable. |
| Keyboard use | Copy buttons work with keyboard activation and have visible focus. |
| Loading and error announcements | Appropriate status/alert semantics and `aria-busy` transitions are present. |
| Real endpoint, when available | Actual backend results render and copy; failure states work with the confirmed integration. |
| Browser console | No uncaught errors or missing module/style files during the exercised flow. |
| Privacy | No added persistence or frontend API key; privacy note remains visible. |

For integrated demo testing, also work with Person 3 to check empty input and each of the four tones. Test example messages about asking a professor for help, following up with a teammate, declining an invitation, and making a professional request. Confirm the output preserves intent and reflects the selected tone; route generation issues to the backend owners.

Fix issues within Person 4's output and styling work. Report input/request/backend problems to the relevant owner without silently replacing their implementation.

## 14. Recommended implementation order

1. Read the PRD, frontend plan, current code, and applicable repository instructions.
2. Confirm `#results`, script-loading conventions, input styling selectors, and the three-function interface with Person 3.
3. Create `output.js` and implement the loading/error/result state transitions.
4. Render and validate three result cards using the sample response.
5. Add copy behavior, success feedback, and clipboard failure handling.
6. Build the shared styling and phone layout, then adapt it for laptop widths.
7. Verify the output independently with the one-second mock response and malformed data.
8. Integrate with Person 3's submit handler and confirm loading/button/error behavior together.
9. Verify the real response when the endpoint is ready and document any dependency still outstanding.
10. Complete responsive, keyboard, copy, retry, and demo checks; fix remaining output issues.
11. Give the team the final handoff and focused change list.

Get the complete render-and-copy flow working before optional visual polish. Keep enhancements within the MVP scope.

## 15. Final deliverables and handoff checklist

- [ ] `frontend/output.js` provides `renderResults(rewrites)`, `showLoading()`, and `showError(msg)` through the agreed loading approach.
- [ ] Valid responses produce exactly three safe, readable, selectable cards.
- [ ] Each card copies its own exact text and briefly shows **Copied!** after success.
- [ ] Clipboard failures preserve results and explain how to copy manually.
- [ ] Loading, request errors, retries, and replacement of old results work correctly.
- [ ] `frontend/styles.css` covers the existing input UI, output UI, and responsive layout.
- [ ] Keyboard focus, readable contrast, status announcements, and long-text handling are checked.
- [ ] The one-second mock fixture is verified and clearly distinguished from real backend output.
- [ ] Person 3 has the import/container requirements and request-state integration example.
- [ ] Backend URL, CORS needs, response shape, and error format are recorded or identified as pending.
- [ ] No message persistence or frontend API key has been introduced.
- [ ] Verification results state what actually passed and what awaits teammates.

The final handoff should name the changed files, describe the public functions, state the local serving/verification steps, and list any remaining integration dependency. Do not mark the complete live MVP as done unless the real AI rewrite flow has been verified.

If committing is requested, inspect the diff and stage only the specific Person 4 files and any coordinated frontend edits. `docs/frontend-plan.md` cautions against `git add .` because unrelated backend files may be present. Leave unrelated backend changes, generated files, and teammate work untouched. Do not commit or push unless the user has requested it.
