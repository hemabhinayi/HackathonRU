const INVALID_RESULTS = "We couldn't load the rewrite options. Please try again.";
const DEFAULT_ERROR = 'Something went wrong. Please try again.';
const COPY_ERROR = "Couldn't copy automatically. Select the text and copy it manually.";
const FEEDBACK_DURATION = 1500;

let mounted;
const cleanups = new Set();

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function clearCards() {
  for (const cleanup of cleanups) cleanup();
  cleanups.clear();
}

function output() {
  const root = document.getElementById('results');
  if (!root) throw new Error('SayLess output requires a #results container.');
  if (mounted?.root === root) return mounted;

  clearCards();
  const status = element('p', 'output-status sr-only');
  status.setAttribute('role', 'status');
  status.setAttribute('aria-live', 'polite');
  status.setAttribute('aria-atomic', 'true');
  const error = element('div', 'output-error');
  error.setAttribute('role', 'alert');
  error.setAttribute('aria-atomic', 'true');
  const content = element('div', 'output-content');
  root.replaceChildren(status, error, content);
  root.setAttribute('aria-busy', 'false');
  mounted = { root, status, error, content };
  return mounted;
}

function setState(state) {
  const ui = output();
  clearCards();
  ui.content.replaceChildren();
  ui.error.replaceChildren();
  ui.status.textContent = '';
  ui.status.className = 'output-status sr-only';
  ui.root.dataset.state = state;
  ui.root.setAttribute('aria-busy', String(state === 'loading'));
  return ui;
}

function createCard(rewrite, index) {
  const number = index + 1;
  const card = element('article', 'result-card');
  const heading = element('h3', 'result-card__label', `Option ${number}`);
  heading.id = `rewrite-option-${number}`;
  card.setAttribute('aria-labelledby', heading.id);
  const text = element('p', 'result-card__text', rewrite);
  const footer = element('div', 'result-card__footer');
  const button = element('button', 'copy-button', 'Copy');
  button.type = 'button';
  const label = `Copy option ${number}`;
  button.setAttribute('aria-label', label);
  const status = element('p', 'copy-status');
  status.setAttribute('role', 'status');
  status.setAttribute('aria-live', 'polite');
  status.setAttribute('aria-atomic', 'true');
  footer.append(button, status);
  card.append(heading, text, footer);

  let timer;
  let active = true;
  let attempt = 0;
  function resetButton() {
    button.textContent = 'Copy';
    button.setAttribute('aria-label', label);
    button.classList.remove('is-copied');
  }

  async function copy() {
    const currentAttempt = ++attempt;
    clearTimeout(timer);
    resetButton();
    status.textContent = '';
    status.classList.remove('copy-status--error');
    try {
      if (typeof navigator.clipboard?.writeText !== 'function') {
        throw new Error('Clipboard unavailable');
      }
      // Keep this call in the click event so the browser receives user activation.
      await navigator.clipboard.writeText(rewrite);
      if (!active || !card.isConnected || currentAttempt !== attempt) return;
      button.textContent = 'Copied!';
      button.setAttribute('aria-label', `Copied option ${number}!`);
      button.classList.add('is-copied');
      status.textContent = `Option ${number} copied to clipboard.`;
      timer = setTimeout(() => {
        resetButton();
        status.textContent = '';
      }, FEEDBACK_DURATION);
    } catch {
      if (!active || !card.isConnected || currentAttempt !== attempt) return;
      resetButton();
      status.classList.add('copy-status--error');
      status.textContent = COPY_ERROR;
    }
  }

  button.addEventListener('click', copy);
  cleanups.add(() => {
    active = false;
    clearTimeout(timer);
    button.removeEventListener('click', copy);
  });
  return card;
}

/** Replace the output with exactly three nonblank strings, preserving their text. */
export function renderResults(rewrites) {
  // Array.from exposes holes in sparse arrays so they fail the string check.
  if (!Array.isArray(rewrites) || rewrites.length !== 3 ||
      !Array.from(rewrites).every((value) => typeof value === 'string' && value.trim())) {
    showError(INVALID_RESULTS);
    return;
  }
  const ui = setState('results');
  const header = element('div', 'results-heading');
  header.append(
    element('h2', '', 'Your rewrites'),
    element('p', '', 'Three ways to say it. Pick the one that sounds like you.')
  );
  const cards = element('div', 'results-grid');
  rewrites.forEach((rewrite, index) => cards.append(createCard(rewrite, index)));
  ui.content.append(header, cards);
  ui.status.textContent = '3 rewrite options ready.';
}

/** Replace old results or an error with the pending state. */
export function showLoading() {
  const ui = setState('loading');
  ui.status.className = 'output-status status-panel';
  ui.status.textContent = 'Rewriting...';
}

/** Display a caller-supplied, user-facing error as plain text. */
export function showError(msg) {
  const ui = setState('error');
  ui.error.append(
    element('strong', '', 'Something needs your attention'),
    element('p', '', typeof msg === 'string' && msg.trim() ? msg : DEFAULT_ERROR)
  );
}

// Module scripts run after parsing. Establish empty live regions before the
// first interaction; calls still mount lazily if the host adds #results later.
if (document.getElementById('results')) output();
