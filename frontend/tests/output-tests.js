import { renderResults, showLoading, showError } from '../output.js';
import { MOCK_RESPONSE, mockRewrite } from '../mock-response.js';

const results = document.querySelector('#results');
const report = document.querySelector('#test-report');
const summary = document.querySelector('#test-summary');
const checks = [];
const uncaught = [];
const clipboardDescriptor = Object.getOwnPropertyDescriptor(navigator, 'clipboard');
const expectedFixture = [
  'Could you let me know when you expect to finish your part?',
  'I wanted to check in on your section so we can stay on schedule.',
  'Can you share an update on your part of the project?',
];
const alternate = ['A fresh first option.', 'A fresh second option.', 'A fresh third option.'];
const delay = (milliseconds) => new Promise((resolve) => setTimeout(resolve, milliseconds));
const flush = () => delay(0);
const cards = () => [...results.querySelectorAll('.result-card')];
const buttons = () => [...results.querySelectorAll('.copy-button')];
const statuses = () => [...results.querySelectorAll('.copy-status')];
const onUnhandled = (event) => uncaught.push(String(event.reason ?? event.error ?? event.message));

window.addEventListener('unhandledrejection', onUnhandled);
window.addEventListener('error', onUnhandled);
document.documentElement.dataset.testStatus = 'running';

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function equal(actual, expected, message) {
  assert(actual === expected, `${message}; expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
}

function setClipboard(value) {
  Object.defineProperty(navigator, 'clipboard', {
    configurable: true,
    value,
  });
}

function restoreClipboard() {
  if (clipboardDescriptor) {
    Object.defineProperty(navigator, 'clipboard', clipboardDescriptor);
  } else {
    delete navigator.clipboard;
  }
}

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((onResolve, onReject) => {
    resolve = onResolve;
    reject = onReject;
  });
  return { promise, resolve, reject };
}

function checkCards(expected = expectedFixture) {
  equal(cards().length, 3, 'Exactly three result cards');
  equal(buttons().length, 3, 'Exactly three copy buttons');
  equal(results.getAttribute('aria-busy'), 'false', 'Results end busy state');
  assert([...results.querySelectorAll('[role="alert"]')].every((alert) => !alert.textContent.trim()), 'No request alert text remains');
  cards().forEach((card, index) => {
    equal(card.querySelector('.result-card__text')?.textContent, expected[index], `Option ${index + 1} text`);
    assert(card.textContent.includes(`Option ${index + 1}`), `Option ${index + 1} has a label`);
    equal(buttons()[index].type, 'button', 'Copy cannot submit a form');
  });
}

function checkRequestError() {
  equal(cards().length, 0, 'Request error removes old cards');
  equal(results.getAttribute('aria-busy'), 'false', 'Request error ends busy state');
  const alert = results.querySelector('[role="alert"]');
  assert(alert?.textContent.trim(), 'Request error has nonempty alert text');
  assert(!results.textContent.includes('Rewriting...'), 'Request error removes loading text');
  return alert;
}

async function check(name, run) {
  const item = document.createElement('li');
  try {
    await run();
    checks.push({ name, passed: true });
    item.dataset.result = 'passed';
    item.textContent = `PASS — ${name}`;
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    checks.push({ name, passed: false, message });
    item.dataset.result = 'failed';
    item.textContent = `FAIL — ${name}: ${message}`;
  } finally {
    restoreClipboard();
    report.append(item);
    summary.textContent = `${checks.filter((entry) => entry.passed).length}/${checks.length} checks passed; running…`;
  }
}

try {
  await check('Initial output is empty and the three public exports are functions', () => {
    [renderResults, showLoading, showError].forEach((fn) => equal(typeof fn, 'function', 'Named public export'));
    equal(results.textContent.trim(), '', 'No fabricated initial output');
    equal(cards().length, 0, 'No initial cards');
  });

  await check('The exact PRD mock fixture resolves after approximately one second', async () => {
    equal(JSON.stringify(MOCK_RESPONSE.rewrites), JSON.stringify(expectedFixture), 'Exported fixture matches the PRD');
    showLoading();
    assert(results.textContent.includes('Rewriting...'), 'Loading is displayed immediately');
    equal(results.getAttribute('aria-busy'), 'true', 'Loading marks results busy');
    const started = performance.now();
    const response = await mockRewrite();
    assert(performance.now() - started >= 900, 'Mock waits roughly one second');
    equal(JSON.stringify(response.rewrites), JSON.stringify(expectedFixture), 'Resolved fixture matches the PRD');
    renderResults(response.rewrites);
    checkCards();
  });

  await check('Loading replaces cards and errors with an accessible pending state', () => {
    renderResults(expectedFixture);
    showLoading();
    equal(cards().length, 0, 'Loading removes old cards');
    equal(results.getAttribute('aria-busy'), 'true', 'Loading sets busy state');
    assert(results.querySelector('[role="status"]')?.textContent.includes('Rewriting...'), 'Loading has a status announcement');
    showError('An earlier request failed.');
    showLoading();
    assert([...results.querySelectorAll('[role="alert"]')].every((alert) => !alert.textContent.trim()), 'Loading clears old alert text');
    assert(!results.textContent.includes('An earlier request failed.'), 'Loading removes old error text');
  });

  await check('A retry and a second response replace old output with exactly three cards', () => {
    showError('Temporary request failure.');
    showLoading();
    renderResults(expectedFixture);
    checkCards();
    const previous = cards()[0];
    renderResults(alternate);
    checkCards(alternate);
    assert(!previous.isConnected, 'Old card is detached');
  });

  await check('Malformed responses, including sparse arrays, show friendly request errors', () => {
    const invalid = [
      undefined, null, {}, { rewrites: expectedFixture }, 'text', [],
      expectedFixture.slice(0, 2), [...expectedFixture, 'Fourth'],
      ['One', 2, 'Three'], ['One', null, 'Three'], ['One', {}, 'Three'],
      ['One', '', 'Three'], ['One', ' \n\t ', 'Three'],
      Array(3), ['One', , 'Three'],
    ];
    invalid.forEach((value) => {
      showLoading();
      renderResults(value);
      checkRequestError();
      assert(!results.textContent.includes('undefined'), 'No undefined value is shown');
      assert(!results.textContent.includes('[object Object]'), 'No object is shown');
    });
  });

  await check('Request errors display literal supplied text and sensible fallbacks', () => {
    const message = 'Request failed: <b>please retry</b> & keep your draft.';
    renderResults(expectedFixture);
    showError(message);
    assert(checkRequestError().textContent.includes(message), 'Supplied message is preserved');
    equal(results.querySelectorAll('b').length, 0, 'Error HTML does not become markup');
    [undefined, null, {}, 42, '', ' \n '].forEach((value) => {
      showLoading();
      showError(value);
      checkRequestError();
    });
  });

  await check('Rewrites preserve literal HTML, whitespace, line breaks, and long content', () => {
    const values = [
      '  Leading and trailing whitespace.\nSecond line.  ',
      '<script>window.__saylessUnexpectedExecution = true</script><b>hello</b>',
      'https://example.test/' + 'x'.repeat(1500),
    ];
    renderResults(values);
    checkCards(values);
    equal(results.querySelectorAll('script, b').length, 0, 'Rewrite markup is rendered as text');
    equal(window.__saylessUnexpectedExecution, undefined, 'Script text is inert');
  });

  await check('Each copy button writes its exact text and has independent success feedback', async () => {
    const values = ['  First.\nLine two.  ', 'Second — punctuation!', '<b>Third</b>'];
    const writes = [];
    setClipboard({ writeText: async (text) => { writes.push(text); } });
    renderResults(values);
    for (let index = 0; index < 3; index += 1) {
      const button = buttons()[index];
      assert(button.getAttribute('aria-label')?.includes(String(index + 1)), 'Accessible copy name identifies its option');
      button.click();
      await flush();
      equal(writes[index], values[index], 'Clipboard receives exact rewrite');
      equal(button.textContent.trim(), 'Copied!', 'Success changes clicked label');
      const status = statuses()[index];
      assert(status?.textContent.trim(), 'Success has status text');
      assert(status.matches('[role="status"], [aria-live="polite"], [aria-live="assertive"]'), 'Copy status announces feedback');
      if (index < 2) equal(buttons()[index + 1].textContent.trim(), 'Copy', 'Unclicked button is unchanged');
    }
    await delay(1700);
    buttons().forEach((button) => equal(button.textContent.trim(), 'Copy', 'Copy label resets after about 1.5 seconds'));
    checkCards(values);
  });

  await check('Copied! appears only after the clipboard promise succeeds', async () => {
    const pending = deferred();
    let calls = 0;
    setClipboard({ writeText: () => { calls += 1; return pending.promise; } });
    renderResults(expectedFixture);
    const button = buttons()[0];
    button.click();
    await flush();
    equal(calls, 1, 'User click initiates clipboard write');
    assert(!button.textContent.includes('Copied!'), 'Pending write is not reported as successful');
    pending.resolve();
    await flush();
    equal(button.textContent.trim(), 'Copied!', 'Resolved write is reported as successful');
  });

  await check('Repeated completed copy clicks reset their feedback timer', async () => {
    setClipboard({ writeText: async () => {} });
    renderResults(expectedFixture);
    const button = buttons()[0];
    button.click();
    await flush();
    await delay(850);
    button.click();
    await flush();
    await delay(850);
    equal(button.textContent.trim(), 'Copied!', 'Earlier feedback timeout does not clear newer feedback');
    await delay(850);
    equal(button.textContent.trim(), 'Copy', 'Latest feedback eventually clears');
  });

  await check('Pending rapid clicks are serialized or protect the latest feedback', async () => {
    const requests = [];
    setClipboard({ writeText: () => {
      const request = deferred();
      requests.push(request);
      return request.promise;
    } });
    renderResults(expectedFixture);
    const button = buttons()[0];
    button.click();
    button.click();
    await flush();
    assert(requests.length === 1 || requests.length === 2, 'Rapid clicks start a predictable number of writes');
    if (requests.length === 2) {
      requests[1].reject(new Error('Latest write denied'));
      await flush();
      const latestStatus = statuses()[0].textContent;
      assert(latestStatus.trim(), 'Latest failure gives guidance');
      requests[0].resolve();
      await flush();
      assert(!button.textContent.includes('Copied!'), 'Older success does not override newer failure');
      equal(statuses()[0].textContent, latestStatus, 'Older success leaves latest failure feedback intact');
    } else {
      requests[0].resolve();
      await flush();
      equal(button.textContent.trim(), 'Copied!', 'Serialized write can complete');
    }
    checkCards();
  });

  await check('Rejected clipboard writes preserve cards and explain manual copying', async () => {
    setClipboard({ writeText: async () => { throw new Error('Permission denied'); } });
    renderResults(expectedFixture);
    buttons()[1].click();
    await flush();
    checkCards();
    assert(!buttons()[1].textContent.includes('Copied!'), 'Failure never claims success');
    assert(/select|manual/i.test(statuses()[1].textContent), 'Failed copy explains manual fallback');
    equal(statuses()[0].textContent.trim(), '', 'Other card feedback remains untouched');
  });

  await check('Unavailable clipboard support preserves cards and explains manual copying', async () => {
    setClipboard(undefined);
    renderResults(expectedFixture);
    buttons()[2].click();
    await flush();
    checkCards();
    assert(!buttons()[2].textContent.includes('Copied!'), 'Missing support never claims success');
    assert(/select|manual/i.test(statuses()[2].textContent), 'Missing clipboard support explains fallback');
  });

  await check('A stale successful clipboard write cannot change loading or new cards', async () => {
    const pending = deferred();
    setClipboard({ writeText: () => pending.promise });
    renderResults(expectedFixture);
    buttons()[0].click();
    showLoading();
    const loadingText = results.textContent;
    pending.resolve();
    await flush();
    equal(results.textContent, loadingText, 'Stale success leaves loading unchanged');
    equal(results.getAttribute('aria-busy'), 'true', 'Stale success leaves loading busy');
    renderResults(alternate);
    checkCards(alternate);
    buttons().forEach((button) => equal(button.textContent.trim(), 'Copy', 'New cards have fresh copy feedback'));
  });

  await check('A stale rejected clipboard write cannot replace a current request error', async () => {
    const pending = deferred();
    setClipboard({ writeText: () => pending.promise });
    renderResults(expectedFixture);
    buttons()[0].click();
    showError('The current request failed.');
    const currentText = results.textContent;
    pending.reject(new Error('Old clipboard request failed'));
    await flush();
    equal(results.textContent, currentText, 'Stale copy rejection leaves request error unchanged');
    checkRequestError();
  });

  await check('Old copy timers cannot reset success on replacement cards', async () => {
    setClipboard({ writeText: async () => {} });
    renderResults(expectedFixture);
    buttons()[0].click();
    await flush();
    await delay(400);
    renderResults(alternate);
    const newButton = buttons()[0];
    newButton.click();
    await flush();
    await delay(1250);
    equal(newButton.textContent.trim(), 'Copied!', 'Old card timer does not clear new card feedback');
    checkCards(alternate);
  });

  await check('Exercised output and clipboard flows have no uncaught errors', async () => {
    await flush();
    equal(uncaught.length, 0, `No uncaught errors (${uncaught.join('; ')})`);
  });
} finally {
  restoreClipboard();
  window.removeEventListener('unhandledrejection', onUnhandled);
  window.removeEventListener('error', onUnhandled);
  const failed = checks.filter((entry) => !entry.passed);
  window.testResults = {
    passed: checks.length - failed.length,
    failed: failed.length,
    checks,
    uncaught,
  };
  document.documentElement.dataset.testStatus = failed.length === 0 ? 'passed' : 'failed';
  summary.textContent = `${checks.length - failed.length}/${checks.length} checks passed.${failed.length ? ' Review failures below.' : ' All functional checks passed.'}`;
}
