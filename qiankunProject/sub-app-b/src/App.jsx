import React from 'react';
import { Routes, Route } from 'react-router-dom';
import List from './pages/List';
import Detail from './pages/Detail';
import Edit from './pages/Edit';

/**
 * 子应用 B 根组件 - 路由配置
 */
function App() {
  return (
    <Routes>
      {/* 列表页 - 默认首页 */}
      <Route path="/" element={<List />} />
      {/* 详情页 */}
      <Route path="/detail" element={<Detail />} />
      {/* 编辑页 */}
      <Route path="/edit" element={<Edit />} />
    </Routes>
  );
}

export default App;
