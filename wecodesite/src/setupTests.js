import '@testing-library/jest-dom';

global.openUrl = jest.fn();
global.queryParams = jest.fn((param) => {
  if (param === 'appId') return 'test-app-id';
  return '';
});

global.message = {
  success: jest.fn(),
  error: jest.fn(),
  warning: jest.fn(),
  info: jest.fn(),
};

if (!window.location) {
  window.location = {};
}
window.location.search = '?appId=test-app-id';
window.location.hash = '';

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn(),
  })),
});

window.getComputedStyle = jest.fn().mockImplementation(() => ({
  getPropertyValue: () => '',
}));
