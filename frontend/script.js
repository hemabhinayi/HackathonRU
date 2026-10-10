import { renderResults, showLoading, showError } from './output.js';
import { mockRewrite } from './mock-response.js';

const form = document.getElementById('rewrite-form');
const messageInput = document.getElementById('message');
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

messageInput.addEventListener('input', () => messageInput.removeAttribute('aria-invalid'));

async function requestRewrite(message, tone) {
  if (mode === 'mock') return mockRewrite();
  const response = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, tone }),
    signal: AbortSignal.timeout(30000)
  });
  if (!response.ok) throw new Error('Rewrite request failed');
  return response.json();
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
  messageInput.removeAttribute('aria-invalid');
  pending = true;
  submitButton.disabled = true;
  for (const button of toneButtons) button.disabled = true;
  showLoading();
  try {
    const data = await requestRewrite(message, selectedTone);
    renderResults(data?.rewrites);
  } catch (error) {
    showError(error.name === 'TimeoutError'
      ? 'That took too long. Please try again.'
      : 'We couldn’t rewrite your message. Please try again.');
  } finally {
    pending = false;
    submitButton.disabled = false;
    for (const button of toneButtons) button.disabled = false;
  }
});
