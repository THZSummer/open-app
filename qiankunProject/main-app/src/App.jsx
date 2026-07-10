import React from 'react';
import { HashRouter, Routes, Route, Link } from 'react-router-dom';
import './index.css';

/**
 * 主应用布局组件
 * 包含侧边导航栏和内容区域
 * 使用 HashRouter，qiankun 子应用从 location.hash 中匹配 activeRule
 */
function App() {
  return (
    <HashRouter>
      <div className="layout">
        {/* 侧边导航栏 */}
        <aside className="sidebar">
          <h1 className="logo">微前端基座</h1>
          <nav>
            <Link to="/" className="nav-link">首页</Link>
            <Link to="/qiankun/sub-b?source=main-app&userId=1001" className="nav-link">子应用 B</Link>
            <Link to="/qiankun/sub-c?source=main-app&userId=1001" className="nav-link">子应用 C</Link>
            <Link to="/qiankun/sub-d?source=main-app&userId=1001" className="nav-link">子应用 D</Link>
            <Link to="/qiankun/sub-e?source=main-app&userId=1001" className="nav-link">子应用 E</Link>
          </nav>
        </aside>
        {/* 内容区域：主应用自身页面 + 子应用挂载容器 */}
        <main className="content">
          {/* 主应用自身路由页面 */}
          <Routes>
            <Route path="/" element={<div className="home">主应用首页 - 欢迎使用 qiankun 微前端架构</div>} />
            {/* qiankun 子应用路由占位，实际渲染由 #sub-app-viewport 容器接管 */}
            <Route path="qiankun/*" element={null} />
          </Routes>
          {/* 子应用挂载容器：qiankun 根据 activeRule 自动把对应子应用渲染到这里 */}
          <div id="sub-app-viewport"></div>
        </main>
      </div>
    </HashRouter>
  );
}

export default App;
