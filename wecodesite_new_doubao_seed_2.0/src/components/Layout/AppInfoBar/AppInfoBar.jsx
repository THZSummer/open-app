import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { HomeOutlined } from '@ant-design/icons';
import './AppInfoBar.m.less';

const AppInfoBar = () => {
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const appId = searchParams.get('appId');

  // 模拟应用数据
  const appInfo = {
    name: '测试应用',
    binding: 'EAMAP-2024-001'
  };

  return (
    <div className="appInfoBar">
      <Link to="/" className="homeLink">
        <HomeOutlined />
      </Link>
      <div className="appInfo">
        <div className="appName">{appInfo.name}</div>
        <div className="appBinding">已绑定：{appInfo.binding}</div>
      </div>
    </div>
  );
};

export default AppInfoBar;