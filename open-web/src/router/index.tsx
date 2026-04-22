import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from '@/components/Layout';
import Welcome from '@/pages/Welcome';
import NotFound from '@/pages/NotFound';

// 分类管理
import CategoryList from '@/pages/category/CategoryList';

// API 管理
import ApiList from '@/pages/api/ApiList';

// 事件管理
import EventList from '@/pages/event/EventList';

// 回调管理
import CallbackList from '@/pages/callback/CallbackList';

// 权限申请
import PermissionApply from '@/pages/permission/PermissionApply';

// 审批中心
import ApprovalCenter from '@/pages/approval/ApprovalCenter';

/**
 * 路由配置
 * 
 * 路由结构：
 * - /: 首页/欢迎页
 * - /categories: 分类管理
 * - /apis: API 管理
 * - /events: 事件管理
 * - /callbacks: 回调管理
 * - /approvals: 审批中心
 * - /404: 404 页面
 */
const Router = () => {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<Welcome />} />
        <Route path="categories" element={<CategoryList />} />
        <Route path="apis" element={<ApiList />} />
        <Route path="events" element={<EventList />} />
        <Route path="callbacks" element={<CallbackList />} />
        <Route path="permissions" element={<PermissionApply />} />
        <Route path="approvals" element={<ApprovalCenter />} />
        <Route path="404" element={<NotFound />} />
        <Route path="*" element={<Navigate to="/404" replace />} />
      </Route>
    </Routes>
  );
};

export default Router;
