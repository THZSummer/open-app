import React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Header from './Header/Header';
import AppInfoBar from './AppInfoBar/AppInfoBar';
import Sidebar from './Sidebar/Sidebar';
import './Layout.m.less';

const Layout = () => {
  const location = useLocation();
  const isHomePage = location.pathname === '/';

  return (
    <div className="layout">
      <Header />
      {!isHomePage && <AppInfoBar />}
      <div className="contentWrapper">
        {!isHomePage && <Sidebar />}
        <div className="content">
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default Layout;