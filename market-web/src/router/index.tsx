import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from '@/components/Layout';
import Welcome from '@/pages/Welcome';
import NotFound from '@/pages/NotFound';

// LookUp 管理
import ClassifyList from './routeRedBlue/lookup-classify';
import ItemList from './routeRedBlue/lookup-item';

// 数据字典管理
import DictionaryList from './routeRedBlue/lookup-dictionary';

// 审批管理
import Approval from './routeRedBlue/approval';

// 机器人绑定
import ApprovalDetail from './routeRedBlue/approveDetail';

// 能力目录管理
import AbilityAdminList from './routeRedBlue/ability-admin';

/**
 * 路由配置
 *
 * 路由结构：
 * - /: 首页/欢迎页
 * - /lookup-classify: LookUp 分类管理
 * - /lookup-item: LookUp 项管理
 * - /dictionary: 数据字典管理
 * - /approval: 审批管理
 * - /approveDetail: 机器人绑定页面
 * - /ability-admin: 能力目录管理
 * - /404: 404 页面
 */
const Router = () => {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Welcome />} />
        <Route path="lookup-classify" element={<ClassifyList />} />
        <Route path="lookup-item" element={<ItemList />} />
        <Route path="dictionary" element={<DictionaryList />} />
        <Route path="approval" element={<Approval />} />
        <Route path="approveDetail" element={<ApprovalDetail />} />
        <Route path="ability-admin" element={<AbilityAdminList />} />
        <Route path="404" element={<NotFound />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Route>
    </Routes>
  );
};

export default Router;
