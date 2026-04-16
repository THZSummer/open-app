import React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Layout as AntLayout } from 'antd';
import Header from './Header/Header';
import AppInfoBar from './AppInfoBar/AppInfoBar';
import Sidebar from './sidebar/Sidebar';
import './Layout.m.less';

const { Content, Sider } = AntLayout;

const HEADER_HEIGHT = 64;
const APP_INFO_BAR_HEIGHT = 50;
const SIDE_PADDING = 16;
const PADDING = 24;

function Layout() {
  const location = useLocation();
  const isDetailPage = location.pathname !== '/';
  
  const contentAreaHeight = isDetailPage 
    ? `calc(100vh - ${HEADER_HEIGHT}px - ${APP_INFO_BAR_HEIGHT}px - ${PADDING}px)`
    : `calc(100vh - ${HEADER_HEIGHT}px - ${PADDING}px)`;

  const sidebarMainHeight = isDetailPage 
    ? `calc(100vh - ${HEADER_HEIGHT}px - ${APP_INFO_BAR_HEIGHT}px - ${PADDING}px)`
    : `calc(100vh - ${HEADER_HEIGHT}px - ${PADDING}px)`;

  return (
    <AntLayout style={{ minHeight: '100vh', overflow: 'hidden' }}>
      <Header style={{ flexShrink: 0 }} />
      {isDetailPage && <AppInfoBar style={{ flexShrink: 0 }} />}
      <AntLayout style={{ flex: 1, flexDirection: 'row', overflow: 'hidden' }}>
        {isDetailPage && (
          <Sider 
            width={220}
            className="sidebar"
            style={{ 
              background: '#fff', 
              borderRight: '1px solid #f0f0f0', 
              overflowY: 'auto',
              overflowX: 'hidden',
              flexShrink: 0,
              height: sidebarMainHeight,
              padding: `${SIDE_PADDING}px`,
              boxSizing: 'border-box'
            }}
          >
            <Sidebar sidebarMainHeight={sidebarMainHeight} />
          </Sider>
        )}
        <Content style={{ 
          background: 'linear-gradient(180deg, #f7f9fc 0%, #f0f4f9 100%)', 
          padding: PADDING,
          height: sidebarMainHeight,
          overflowY: 'auto',
          overflowX: 'hidden',
          boxSizing: 'border-box'
        }}>
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  );
}

export default Layout;