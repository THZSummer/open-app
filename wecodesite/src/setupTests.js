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

// 通过 history API 初始化查询参数，避免 jsdom 直接设置 location.search 触发导航未实现报错。
window.history.replaceState({}, '', '/?appId=test-app-id');

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
