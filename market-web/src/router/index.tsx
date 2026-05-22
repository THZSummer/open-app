import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from '@/components/Layout';
import Welcome from '@/pages/Welcome';
import NotFound from '@/pages/NotFound';

// LookUp 管理
import ClassifyList from './routeRedBlue/lookup-classify';
import ItemList from './routeRedBlue/lookup-item';
import TaskList from './routeRedBlue/task-center';

// 数据字典管理
import DictionaryList from '@/pages/dictionary';

/**
 * 路由配置
 *
 * 路由结构：
 * - /: 首页/欢迎页
 * - /lookup-classify: LookUp 分类管理
 * - /lookup-item: LookUp 项管理
 * - /task-center: 任务中心
 * - /dictionary: 数据字典管理
 * - /404: 404 页面
 */
const Router = () => {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Welcome />} />
        <Route path="lookup-classify" element={<ClassifyList />} />
        <Route path="lookup-item" element={<ItemList />} />
        <Route path="task-center" element={<TaskList />} />
        <Route path="dictionary" element={<DictionaryList />} />
        <Route path="404" element={<NotFound />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Route>
    </Routes>
  );
};

export default Router;
