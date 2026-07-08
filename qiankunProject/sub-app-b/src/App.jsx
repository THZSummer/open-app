import React from 'react';
import './App.css';

/**
 * 子应用 B 根组件
 */
function App() {
  return (
    <div className="sub-app-b">
      <h2>子应用 B（React + Vite）</h2>
      <p>这是通过 qiankun 嵌入到主应用中的子应用页面。</p>
      <p>技术栈：React 18 + Vite 5</p>
    </div>
  );
}

export default App;
