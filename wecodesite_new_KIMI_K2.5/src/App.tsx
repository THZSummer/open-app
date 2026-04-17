import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/lib/locale/zh_CN';
import Layout from './components/Layout/Layout';
import AppList from './pages/AppList/AppList';
import BasicInfo from './pages/BasicInfo/BasicInfo';
import Members from './pages/Members/Members';
import Capabilities from './pages/Capabilities/Capabilities';
import CapabilityDetail from './pages/CapabilityDetail/CapabilityDetail';
import ApiManagement from './pages/ApiManagement/ApiManagement';
import Events from './pages/Events/Events';
import OperationLog from './pages/OperationLog/OperationLog';
import VersionRelease from './pages/VersionRelease/VersionRelease';
import VersionForm from './pages/VersionRelease/VersionForm';

const App: React.FC = () => {
  return (
    <ConfigProvider locale={zhCN}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Layout />}>
            <Route index element={<AppList />} />
          </Route>
          <Route 
            path="/" 
            element={
              <Layout showAppInfo={true} />
            }
          >
            <Route path="basic-info" element={<BasicInfo />} />
            <Route path="members" element={<Members />} />
            <Route path="capabilities" element={<Capabilities />} />
            <Route path="capability-detail" element={<CapabilityDetail />} />
            <Route path="api-management" element={<ApiManagement />} />
            <Route path="events" element={<Events />} />
            <Route path="operation-log" element={<OperationLog />} />
            <Route path="version-release" element={<VersionRelease />} />
            <Route path="version-release/form" element={<VersionForm />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
};

export default App;
