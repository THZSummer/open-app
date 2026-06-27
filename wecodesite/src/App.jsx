import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/lib/locale/zh_CN';
import { Provider } from 'react-redux';
import store from './store';
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
import OperationLog from './pages/OperationLog/OperationLog';
import RunManagement from './pages/RunManagement/index';
import CategoryList from './pages/Admin/Category/index';
import ApiList from './pages/Admin/Api/index';
import EventList from './pages/Admin/Event/index';
import CallbackList from './pages/Admin/Callback/index';
import ApprovalCenter from './pages/Admin/Approval/index';
import ConnectorList from './pages/ConnectPlatform/Connector/index';
import ConnectorEditor from './pages/ConnectPlatform/ConnectorEditor/index';
import FlowList from './pages/ConnectPlatform/Flow/index';
// import FlowEditor from './pages/ConnectPlatform/FlowEditor/index';
import FlowEditorV2 from './pages/ConnectPlatform/FlowEditorV2/index';
import 'antd/dist/antd.css';

function App() {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#0066ff',
          borderRadius: 6,
        },
      }}
    >
      <Provider store={store}>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route path="appList" element={<AppList />} />
          <Route path="basic-info" element={<BasicInfo />} />
          <Route path="members" element={<Members />} />
          <Route path="capabilities" element={<Capabilities />} />
          <Route path="capability-detail" element={<CapabilityDetail />} />
          <Route path="api-management" element={<ApiManagement />} />
          <Route path="events" element={<Events />} />
          <Route path="callbacks" element={<Callbacks />} />
          <Route path="operation-log" element={<OperationLog />} />
          <Route path="run-management" element={<RunManagement />} />
          <Route path="version-release" element={<VersionRelease />} />
          {/* ===== 灰度发布：新页面路由（v2 占位，先复用 v1 源码，后续手动替换） ===== */}
          <Route path="basic-info-v2" element={<BasicInfo />} />
          <Route path="members-v2" element={<Members />} />
          <Route path="capabilities-v2" element={<Capabilities />} />
          <Route path="capability-detail-v2" element={<CapabilityDetail />} />
          <Route path="version-release-v2" element={<VersionRelease />} />
          <Route path="operation-log-v2" element={<OperationLog />} />
          <Route path="admin/categories" element={<CategoryList />} />
          <Route path="admin/apis" element={<ApiList />} />
          <Route path="admin/events" element={<EventList />} />
          <Route path="admin/callbacks" element={<CallbackList />} />
          <Route path="admin/approvals" element={<ApprovalCenter />} />
          <Route path="connectorList" element={<ConnectorList />} />
          <Route path="connectorEditor" element={<ConnectorEditor />} />
          <Route path="flowList" element={<FlowList />} />
          {/* V2 连接流编辑器（步骤条形态） */}
          <Route path="flowEditor" element={<FlowEditorV2 />} />
          {/* 旧版连接流编辑器（画布拖拽形态，保留供管理使用） */}
          {/* <Route path="connect/history/flows/editor" element={<FlowEditor />} /> */}
          <Route path="*" element={<Navigate to="/appList" replace />} />
        </Route>
      </Routes>
      </Provider>
    </ConfigProvider>
  );
}

export default App;