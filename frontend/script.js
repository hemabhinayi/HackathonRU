import { renderResults, showLoading, showError } from './output.js';
import { mockRewrite } from './mock-response.js';

const MAX_MESSAGE_LENGTH = 2000;
const GENERIC_ERROR = 'We couldn’t rewrite your message. Please try again.';
// Only these fixed messages are ever shown; server text is never displayed directly.
const ERROR_MESSAGES = {
  VALIDATION_ERROR: 'Enter a message of 1–2000 characters and choose a supported tone.',
  AI_NOT_CONFIGURED: 'Rewriting isn’t available yet. Please try again later.',
  AI_RATE_LIMITED: 'Rewriting is busy right now. Please wait a moment and try again.',
  AI_UNAVAILABLE: 'Rewriting is temporarily unavailable. Please try again later.',
  AI_TIMEOUT: 'That took too long. Please try again.',
  AI_REFUSED: 'We couldn’t rewrite that message. Try rephrasing it.',
  AI_INVALID_RESPONSE: 'We couldn’t create three rewrite options. Please try again.'
};

class RewriteError extends Error {
  constructor(code) {
    super(code);
    this.code = code;
  }
}

const form = document.getElementById('rewrite-form');
const messageInput = document.getElementById('message');
const lengthHint = document.getElementById('message-length');
const submitButton = document.getElementById('rewrite-button');
const toneButtons = [...form.querySelectorAll('[data-tone]')];
const mode = document.querySelector('meta[name="rewrite-mode"]')?.content ?? 'api';
const endpoint = document.querySelector('meta[name="rewrite-endpoint"]')?.content || '/api/rewrite';
let selectedTone = 'professional';
let pending = false;

document.getElementById('mock-notice').hidden = mode !== 'mock';

for (const button of toneButtons) {
  button.addEventListener('click', () => {
    if (pending) return;
    selectedTone = button.dataset.tone;
    for (const option of toneButtons) {
      option.setAttribute('aria-pressed', String(option === button));
    }
  });
}

function updateLengthHint() {
  // String length counts UTF-16 units, matching the server's limit.
  const length = messageInput.value.trim().length;
  lengthHint.textContent = `${length} / ${MAX_MESSAGE_LENGTH} characters`;
  lengthHint.classList.toggle('field-hint--error', length > MAX_MESSAGE_LENGTH);
}

messageInput.addEventListener('input', () => {
  messageInput.removeAttribute('aria-invalid');
  updateLengthHint();
});
updateLengthHint();

async function requestRewrite(message, tone) {
  if (mode === 'mock') return mockRewrite();
  const response = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, tone }),
    signal: AbortSignal.timeout(30000)
  });
  if (!response.ok) {
    let code;
    try {
      code = (await response.json())?.error?.code;
    } catch {
      // Non-JSON error body; fall back to the generic message.
    }
    throw new RewriteError(code);
  }
  return response.json();
}

function errorMessage(error) {
  if (error.name === 'TimeoutError') return ERROR_MESSAGES.AI_TIMEOUT;
  if (error instanceof RewriteError && Object.hasOwn(ERROR_MESSAGES, error.code)) {
    return ERROR_MESSAGES[error.code];
  }
  return GENERIC_ERROR;
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  if (pending) return;
  const message = messageInput.value.trim();
  if (!message) {
    messageInput.setAttribute('aria-invalid', 'true');
    showError('Please enter a message you’d like to rewrite.');
    messageInput.focus();
    return;
  }
  if (message.length > MAX_MESSAGE_LENGTH) {
    messageInput.setAttribute('aria-invalid', 'true');
    showError(`Please shorten your message to ${MAX_MESSAGE_LENGTH} characters or fewer.`);
    messageInput.focus();
    return;
  }
  messageInput.removeAttribute('aria-invalid');
  pending = true;
  submitButton.disabled = true;
  for (const button of toneButtons) button.disabled = true;
  showLoading();
  try {
    const data = await requestRewrite(message, selectedTone);
    renderResults(data?.rewrites);
  } catch (error) {
    showError(errorMessage(error));
  } finally {
    pending = false;
    submitButton.disabled = false;
    for (const button of toneButtons) button.disabled = false;
  }
});
