# SayLess — Product Requirements Document

**Tagline:** Say what you mean, in the tone you want.  
**Hackathon build window:** 10 hours  
**Team:** 4 people  
**Category:** Maverick

## 1. Product summary

SayLess is a web app that rewrites a user's draft message in a selected tone. It helps people express the same intent more clearly, politely, confidently, or humorously.

A user enters a message, selects a tone, and receives three rewrite options they can copy.

## 2. Problem

People often know what they want to say but struggle to phrase it for a specific situation. They may want to sound professional without being stiff, or firm without being rude.

## 3. Target users

Students and others who need help wording everyday messages, such as:

- Asking a professor for help
- Following up with a group-project teammate
- Politely declining an invitation
- Making a request sound more professional

## 4. Core user flow

1. The user opens SayLess.
2. The user enters a message.
3. The user selects a tone.
4. The user clicks **Rewrite my message**.
5. SayLess displays three rewritten options.
6. The user copies the option they like.

## 5. MVP requirements

### Must have

- A text box for the message
- Tone choices: **Professional**, **Casual**, **Diplomatic**, and **Funny**
- A rewrite button
- Three distinct rewritten options that preserve the original intent
- A copy button for each option
- A loading state while results are generated
- Clear error messages if the request fails
- A responsive layout for phones and laptops

### Privacy

- Do not save messages or results.
- Show a short note that messages are sent to an AI service to generate rewrites.
- Remind users not to enter sensitive personal information.

## 6. Out of scope for the hackathon MVP

- User accounts or login
- Database or message history
- Favorites
- Document uploads
- Multiple languages
- A chat assistant
- Fine-tuning or training an AI model

## 7. Technology

- **Frontend:** HTML, CSS, JavaScript
- **Backend:** Java with Spring Boot
- **AI:** A hosted AI API called from the Java backend
- **Database:** None

The frontend sends the message and selected tone to the Java backend. The backend calls the AI API and returns three rewrites. Keep the AI API key on the backend.

## 8. Basic API contract

**Endpoint:** `POST /api/rewrite`

**Request:**

```json
{
  "message": "I need my teammate to finish their part",
  "tone": "professional"
}
```

**Response:**

```json
{
  "rewrites": [
    "Could you let me know when you expect to finish your part?",
    "I wanted to check in on your section so we can stay on schedule.",
    "Can you share an update on your part of the project?"
  ]
}
```

The backend should reject empty messages and return a useful error if the AI service is unavailable.

## 9. Acceptance criteria

The MVP is ready to demo when:

- A user can enter a message and select a tone.
- Clicking the rewrite button displays three results from the AI service.
- The results keep the original meaning while matching the selected tone.
- Each result can be copied.
- The page shows loading and error states.
- The AI API key is not exposed in browser code.
- Messages are not saved by the app.

## 10. Team split

| Person | Ownership |
|---|---|
| 1 | Java Spring Boot endpoint and AI API connection |
| 2 | Backend support: request validation, response handling, integration testing |
| 3 | Frontend layout, input form, tone selector |
| 4 | Result cards, copy buttons, responsive styling, demo testing |

Agree on the request and response JSON before building the frontend and backend separately.

## 11. Ten-hour build plan

| Time | Goal |
|---|---|
| Hour 0–1 | Create the project, agree on the API contract, sketch the page |
| Hours 1–3 | Build the frontend and Java endpoint independently |
| Hours 3–5 | Connect the backend to the AI API and return three rewrites |
| Hours 5–7 | Integrate the frontend and backend; add copy, loading, and error states |
| Hours 7–8 | Test example messages and fix major bugs |
| Hours 8–10 | Polish the interface, prepare the demo, and rehearse the pitch |

**Scope rule:** Get one complete rewrite flow working first. Add extras only after that works reliably.

