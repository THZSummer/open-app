import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import Layout from './components/Layout/Layout';
import AppList from './pages/AppList/AppList';
import BasicInfo from './pages/BasicInfo/BasicInfo';
import Members from './pages/Members/Members';
import Capabilities from './pages/Capabilities/Capabilities';
import CapabilityDetail from './pages/CapabilityDetail/CapabilityDetail';
import ApiManagement from './pages/ApiManagement/index';
import Events from './pages/Events/index';
import Callbacks from './pages/Callbacks/index';
import VersionRelease from './pages/VersionRelease/VersionRelease';
import VersionForm from './pages/VersionRelease/VersionForm';
import OperationLog from './pages/OperationLog/OperationLog';
import CategoryList from './pages/Admin/Category/index';
import ApiList from './pages/Admin/Api/index';
import EventList from './pages/Admin/Event/index';
import CallbackList from './pages/Admin/Callback/index';
import ApprovalCenter from './pages/Admin/Approval/index';
import 'antd/dist/antd.css';

function App() {
  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: '#0066ff',
          borderRadius: 6,
        },
      }}
    >
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<AppList />} />
          <Route path="basic-info" element={<BasicInfo />} />
          <Route path="members" element={<Members />} />
          <Route path="capabilities" element={<Capabilities />} />
          <Route path="capability-detail" element={<CapabilityDetail />} />
          <Route path="api-management" element={<ApiManagement />} />
          <Route path="events" element={<Events />} />
          <Route path="callbacks" element={<Callbacks />} />
          <Route path="operation-log" element={<OperationLog />} />
          <Route path="version-release" element={<VersionRelease />} />
          <Route path="version-release/form" element={<VersionForm />} />
          <Route path="admin/categories" element={<CategoryList />} />
          <Route path="admin/apis" element={<ApiList />} />
          <Route path="admin/events" element={<EventList />} />
          <Route path="admin/callbacks" element={<CallbackList />} />
          <Route path="admin/approvals" element={<ApprovalCenter />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </ConfigProvider>
  );
}

export default App;