import { renderResults, showLoading, showError } from './output.js';
import { mockRewrite } from './mock-response.js';

const previewButton = document.getElementById('mock-rewrite');
let previewRequest = 0;

previewButton.addEventListener('click', async () => {
  const request = ++previewRequest;
  previewButton.disabled = true;
  showLoading();
  try {
    const response = await mockRewrite();
    if (request === previewRequest) renderResults(response.rewrites);
  } catch {
    if (request === previewRequest) showError('We could not rewrite your message. Please try again.');
  } finally {
    if (request === previewRequest) previewButton.disabled = false;
  }
});

function interruptPreview() {
  previewRequest += 1;
  previewButton.disabled = false;
}

document.getElementById('preview-error').addEventListener('click', () => {
  interruptPreview();
  showError('We could not rewrite your message. Please try again.');
});

document.getElementById('preview-long').addEventListener('click', () => {
  interruptPreview();
  renderResults([
    'Hello Professor,\n\nCould we arrange a time to go over the assignment? I would appreciate your guidance.\n\nThank you!',
    `Here is the reference we discussed:\nhttps://example.test/${'a-long-reference-'.repeat(18)}`,
    'Thank you for the invitation! I will not be able to make it this time, but I hope you have a lovely evening.\n\n<b>This is literal text, not HTML.</b>'
  ]);
});

document.querySelectorAll('[data-tone]').forEach((button) => {
  button.addEventListener('click', () => {
    document.querySelectorAll('[data-tone]').forEach((tone) => {
      tone.setAttribute('aria-pressed', String(tone === button));
    });
  });
});
