import React from 'react';
import './App.css';

/**
 * 子应用 C 根组件
 */
function App() {
  return (
    <div className="sub-app-c">
      <h2>子应用 C（React + Webpack）</h2>
      <p>这是通过 qiankun 嵌入到主应用中的子应用页面。</p>
      <p>技术栈：React 18 + Webpack 5</p>
      <p>新增文案</p>
    </div>
  );
}

export default App;
