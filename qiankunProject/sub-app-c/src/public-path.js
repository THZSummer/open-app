/**
 * 动态设置 publicPath
 * 被主应用加载时，qiankun 会注入子应用资源地址，避免静态资源加载 404
 * 注意：此文件必须在入口最前面引入
 */
if (window.__POWERED_BY_QIANKUN__) {
  // eslint-disable-next-line no-undef
  __webpack_public_path__ = window.__INJECTED_PUBLIC_PATH_BY_QIANKUN__;
}
