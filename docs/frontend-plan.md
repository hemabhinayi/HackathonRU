# SayLess Frontend Plan

> Historical frontend split. For the current four-person backend phase, use [the backend plan](backend-plan.md); its ownership and integration contract supersede the backend assignments below.

The frontend is split between Person 3 (input side) and Person 4 (output side) so that you don't edit the same files.

## Setup (Person 3, about 5 minutes)

Create a `frontend/` folder with three empty files: `index.html`, `styles.css`, `script.js`. Commit and push the skeleton first so Person 4 can pull it.

## Person 3: input side

- `index.html`:
  - Page title and tagline.
  - Text box for the message.
  - Four tone buttons: Professional, Casual, Diplomatic, Funny.
  - A "Rewrite my message" button.
  - Privacy note: messages are sent to an AI service, and users shouldn't enter sensitive personal information.
  - An empty `<div id="results"></div>` under the form for Person 4 to fill.
- `script.js`:
  - On button click, read the message and the selected tone.
  - `POST` them to `/api/rewrite` as `{ "message": "...", "tone": "..." }`. Tone values are lowercase, e.g. `"professional"`.
  - Block empty messages with a friendly error before calling the backend.

## Person 4: output side

- Result cards: take the `rewrites` array from the response and render three cards.
- A copy button on each card (`navigator.clipboard.writeText`) that briefly shows "Copied!".
- Loading state (spinner or "Rewriting...") and error message display.
- `styles.css`: responsive layout for phones and laptops, plus the overall look.

## Agree on before splitting

- Person 4 exports `renderResults(rewrites)`, `showLoading()` and `showError(msg)`. Person 3 calls them from the click handler.
- Use a mock response until the backend is ready, so nobody is blocked. Return the three sample rewrites from the PRD after a 1 second delay. Switch to the real `fetch` once Persons 1 and 2 have the stub endpoint up.
- Ask Persons 1 and 2 for the backend URL (probably `http://localhost:8080`) and to enable CORS, or the browser will block the calls.

## API contract (from the PRD)

`POST /api/rewrite`

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

## Git notes

- `backend/` may show as untracked on the `frontend` branch. Don't run `git add .` there, or the backend scaffold gets committed to `frontend`. Add only `frontend/` and `docs/` files.
- Persons 1 and 2 commit `backend/` on the `backend` branch.
