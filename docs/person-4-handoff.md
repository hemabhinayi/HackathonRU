# Person 4 handoff — SayLess

> Historical frontend handoff. Current backend ownership and planned Gemini integration are defined in [the four-person backend plan](backend-plan.md).

Person 4's output module, responsive stylesheet, explicit mock preview, and browser checks are implemented. Person 3 has now integrated the main input page and request handler; see [Person 3 handoff](person-3-handoff.md) for local run instructions and API configuration. The backend still contains only the Spring Boot application scaffold, so real AI results remain pending with Persons 1 and 2.

## Files

| File | Purpose |
|---|---|
| `frontend/output.js` | Three named ES module exports; output state transitions, validation, safe result cards, clipboard feedback and cleanup. |
| `frontend/styles.css` | Page, input-control and output styling; responsive cards, wrapping, visible focus, selected/disabled/error states. |
| `frontend/mock-response.js` | Exact PRD fixture returned after one second, for development only. |
| `frontend/dev-output.html` and `dev-output.js` | Clearly labeled preview with read-only sample input and controls for mock success, error and long text. |
| `frontend/tests/output-tests.html` and `output-tests.js` | Browser-native functional checks; no test framework or build step. |

## Run the preview and checks

From the repository root:

```sh
python3 -m http.server 5500 --bind 127.0.0.1 --directory frontend
```

Open `http://127.0.0.1:5500/dev-output.html` for the preview, or `http://127.0.0.1:5500/tests/output-tests.html` for the checks. The checks take roughly ten seconds and show their result on the page. Stop the server with Ctrl+C when finished.

Use HTTP on localhost or HTTPS, rather than opening the HTML as a file. Module imports and clipboard behavior need a suitable browser context. The preview uses fixed sample data; choosing a tone demonstrates styling only. It makes no network request to an AI service.

## Person 3 integration

Add the stylesheet, the output container below the form, and one module script to the production HTML:

```html
<link rel="stylesheet" href="./styles.css">
<!-- Existing title, input form, tone controls and privacy note go here. -->
<div id="results"></div>
<script type="module" src="./script.js"></script>
```

Import these functions into `script.js`:

```js
import { renderResults, showLoading, showError } from './output.js';
```

| Function | Contract |
|---|---|
| `showLoading()` | Immediately removes previous results/errors and shows `Rewriting...`; sets `aria-busy="true"`. |
| `renderResults(rewrites)` | Takes exactly three nonblank strings, preserves their text, and replaces old output. Invalid arrays show a friendly error. Ends loading. |
| `showError(message)` | Displays a user-facing string as plain text, with a fallback for missing/invalid messages. Removes old cards and ends loading. |

The module owns all markup inside `#results`, including its empty status/alert regions. Do not clear that container from the request handler. There is no extra initialization or hide-loading call to make.

Inside the existing validated submit handler, use this sequence. `submitButton`, `message`, `tone`, and `requestRewrite` below represent Person 3's actual variables and request helper, not preexisting globals:

```js
// Prevent form navigation, reject blank input, and guard duplicate requests first.
submitButton.disabled = true;
showLoading();
try {
  const data = await requestRewrite(message, tone);
  renderResults(data.rewrites);
} catch {
  showError('We could not rewrite your message. Please try again.');
} finally {
  submitButton.disabled = false;
}
```

While the backend is unavailable, import `mockRewrite` from `./mock-response.js` and use it as the request helper. It returns the exact PRD `{ rewrites: [...] }` fixture after one second. Keep this choice explicit; never use mock output as an automatic fallback after a failed real request.

When the endpoint is ready, replace the helper with a real request. For a same-origin deployment, the request helper can begin with:

```js
async function requestRewrite(message, tone) {
  const response = await fetch('/api/rewrite', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, tone })
  });
  if (!response.ok) throw new Error('Rewrite request failed');
  return response.json();
}
```

Use the confirmed backend base URL if it is on a separate origin. Adapt user-facing errors to the agreed backend error format; do not expose stack traces or raw error pages. Preserve the draft and tone on failure. Tone values must be `professional`, `casual`, `diplomatic`, or `funny`.

Guard duplicate submissions in the handler as well as disabling its button. If overlapping requests are deliberately allowed, ignore stale responses. The output module cannot determine which network response is newest; it does guard its own pending clipboard work and feedback timers.

### Styling contract proposed for Person 3

No input selectors existed at implementation time. The following classes are ready for Person 3 to adopt or map onto the final markup; keep any IDs used by the input JavaScript.

| Element | Class or attribute |
|---|---|
| Header / brand | `.site-header`, `.brand`; optional `.brand-mark` |
| Intro / tagline | `.hero`, `.eyebrow`, `.tagline` |
| Form panel | `.composer` or a native `form` |
| Message control | Native `label` and `textarea` |
| Tone group | Native `fieldset`/`legend`, then `.tone-options` or `.tone-selector` |
| Tone buttons | `.tone-button` or `.tone-btn`; `data-tone` also supported |
| Selected tone | `aria-pressed="true"` preferred; `data-tone` buttons also support `.selected` or `.active` |
| Rewrite button | `.primary-button` or `button[type="submit"]` |
| Privacy notice | `.privacy-note` |

The preview contains representative input markup for styling verification, with a read-only sample. It is not the production input handler. Do not import `dev-output.js` into the production page.

## Verification results

Verification was performed in headless Chrome on October 10, 2026.

- All 17 browser-native functional groups passed: exact delayed fixture, replacement of states, invalid and sparse arrays, literal HTML and whitespace, independent copy success, pending/repeated clicks, denied or missing clipboard support, stale promises/timers, and absence of uncaught errors.
- Real browser clipboard writes matched each card's complete text. The browser-native test page uses temporary clipboard substitutes; it restores the original browser property after every group.
- Keyboard Tab navigation showed a visible focus outline on copy controls, and Enter successfully copied a rewrite.
- At 320px, 375px, 768px and 1280px, long and multiline text stayed within each card and the viewport. Copy and preview controls were at least 44px tall; all results and the privacy note remained available.
- All four tone styling samples selected correctly. Preview loading disabled the sample request button, completion restored it, and retry/error/interruption checks passed.
- Reduced-motion mode remained usable; the loading presentation uses text without animation. No browser errors, missing assets, storage entries or cookies were observed in the exercised flow.
- Desktop and phone screenshots were visually reviewed for readability, wrapping and layout. This was Chrome desktop viewport emulation, not a physical-device or cross-browser test.

No persistence, analytics, provider API keys, or direct AI requests were added. Output and errors use `textContent`. The production module writes to the clipboard only after a copy click and does not read the clipboard.

## Remaining team integration checks

These depend on work absent from the checkout and have not been marked as passed:

- **Person 3 complete:** `index.html`/`script.js` use the styling classes and output functions, with input validation and an explicit mock/API request flow. See the Person 3 handoff for verification details.
- **Persons 1 and 2:** confirm the actual backend base URL; `http://localhost:8080` remains an assumption. Implement/confirm `POST /api/rewrite` returning `{ "rewrites": ["first", "second", "third"] }`.
- **Persons 1 and 2:** when origins differ, enable CORS for the exact frontend origin and the JSON POST preflight. `localhost` and `127.0.0.1` are different origins. Do not use `no-cors`.
- **Persons 1 and 2:** confirm the error-response schema and which messages are safe to show.
- **Joint demo check:** replace the mock with the real endpoint; test all four tones with messages about a professor, teammate, invitation, and professional request. Verify actual intent preservation and distinct options; copy the returned results and exercise real network/server failure recovery.
- **Manual accessibility check:** status/alert semantics, keyboard behavior and visible focus have been checked. Actual spoken announcements with VoiceOver or another screen reader still need a manual check, including first loading and error announcements. `aria-busy` can affect when a screen reader announces updates.

The mock proves output behavior and styling. It does not prove real generation quality, CORS, or production form integration. Those dependencies are explicitly retained for the team rather than replacing their implementations.
