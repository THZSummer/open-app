import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import Layout from './components/Layout/Layout';
import AppList from './pages/AppList/AppList';
import BasicInfo from './pages/BasicInfo/BasicInfo';
import Members from './pages/Members/Members';
import Capabilities from './pages/Capabilities/Capabilities';
import CapabilityDetail from './pages/CapabilityDetail/CapabilityDetail';
import ApiManagement from './pages/ApiManagement/ApiManagement';
import Events from './pages/Events/Events';
import VersionRelease from './pages/VersionRelease/VersionRelease';
import VersionForm from './pages/VersionRelease/VersionForm';
import OperationLog from './pages/OperationLog/OperationLog';
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
          <Route path="operation-log" element={<OperationLog />} />
          <Route path="version-release" element={<VersionRelease />} />
          <Route path="version-release/form" element={<VersionForm />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </ConfigProvider>
  );
}

export default App;