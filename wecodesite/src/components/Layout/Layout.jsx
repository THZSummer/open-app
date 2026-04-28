import React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Layout as AntLayout } from 'antd';
import Header from './Header/Header';
import AppInfoBar from './AppInfoBar/AppInfoBar';
import Sidebar from './Sidebar/Sidebar';
import './Layout.m.less';

const { Content, Sider } = AntLayout;

const HEADER_HEIGHT = 64;
const APP_INFO_BAR_HEIGHT = 50;
const SIDE_PADDING = 16;
const PADDING = 24;

const CONTENT_HEIGHT_WITH_APPINFOBAR = `calc(100vh - ${HEADER_HEIGHT}px - ${APP_INFO_BAR_HEIGHT}px - ${PADDING}px)`;
const CONTENT_HEIGHT_WITHOUT_APPINFOBAR = `calc(100vh - ${HEADER_HEIGHT}px - ${PADDING}px)`;
const SIMPLE_CONTENT_HEIGHT = '100vh';

function Layout() {
  const location = useLocation();
  const isAdminPage = location.pathname.startsWith('/admin');
  const isDetailPage = location.pathname !== '/' && !isAdminPage;
  
  const contentAreaHeight = isDetailPage 
    ? CONTENT_HEIGHT_WITH_APPINFOBAR
    : CONTENT_HEIGHT_WITHOUT_APPINFOBAR;

  const sidebarMainHeight = isAdminPage
    ? SIMPLE_CONTENT_HEIGHT
    : (isDetailPage ? CONTENT_HEIGHT_WITH_APPINFOBAR : CONTENT_HEIGHT_WITHOUT_APPINFOBAR);

  const contentHeight = isAdminPage
    ? SIMPLE_CONTENT_HEIGHT
    : (isDetailPage ? CONTENT_HEIGHT_WITH_APPINFOBAR : CONTENT_HEIGHT_WITHOUT_APPINFOBAR);

  return (
    <AntLayout style={{ minHeight: '100vh', overflow: 'hidden' }}>
      {!isAdminPage && <Header style={{ flexShrink: 0 }} />}
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
          background: isAdminPage ? '#fff' : 'linear-gradient(180deg, #f7f9fc 0%, #f0f4f9 100%)', 
          padding: isAdminPage ? 0 : PADDING,
          height: contentHeight,
          overflowY: 'auto',
          overflowX: isAdminPage ? 'auto' : 'hidden',
          boxSizing: 'border-box'
        }}>
          <Outlet />
        </Content>
      </AntLayout>
    </AntLayout>
  );
}

export default Layout;