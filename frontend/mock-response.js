// Development fixture only. The production request handler belongs to Person 3.
export const MOCK_RESPONSE = Object.freeze({
  rewrites: Object.freeze([
    'Could you let me know when you expect to finish your part?',
    'I wanted to check in on your section so we can stay on schedule.',
    'Can you share an update on your part of the project?'
  ])
});

export function mockRewrite() {
  return new Promise((resolve) => {
    setTimeout(() => resolve({ rewrites: [...MOCK_RESPONSE.rewrites] }), 1000);
  });
}
