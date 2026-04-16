import React from 'react';
import { Button } from 'antd';
import AppCard from '../../components/AppCard/AppCard';
import './AppList.m.less';
import mockData from './mock';

const AppList = () => {
  const apps = mockData.apps;

  const handleCreateApp = () => {
    // 触发创建应用流程
    console.log('创建应用');
  };

  return (
    <div className="appList">
      <div className="header">
        <h1 className="title">我的应用</h1>
        <Button type="primary" onClick={handleCreateApp}>
          创建应用
        </Button>
      </div>
      <div className="appGrid">
        {apps.map((app) => (
          <AppCard key={app.id} app={app} />
        ))}
      </div>
    </div>
  );
};

export default AppList;