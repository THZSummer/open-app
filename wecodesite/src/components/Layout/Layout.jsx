import React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Layout as AntLayout } from 'antd';
import { useAppDetail } from '../../contexts/AppContext';
import { useRouteWhitelistGuard } from '../../hooks/useRouteWhitelistGuard';
import Header from './Header/Header';
import AppInfoBar from './AppInfoBar/AppInfoBar';
import Sidebar from './Sidebar/Sidebar';
import './Layout.m.less';

const { Content, Sider } = AntLayout;

const HEADER_HEIGHT = 48;
const APP_INFO_BAR_HEIGHT = 56;
const SIDE_PADDING = 16;
const PADDING = 24;

const CONTENT_HEIGHT_WITH_APPINFOBAR = `calc(100vh - ${HEADER_HEIGHT}px - ${APP_INFO_BAR_HEIGHT}px - ${PADDING}px)`;
const CONTENT_HEIGHT_WITHOUT_APPINFOBAR = `calc(100vh - ${HEADER_HEIGHT}px - ${PADDING}px)`;
const SIMPLE_CONTENT_HEIGHT = '100vh';

function LayoutInner() {
  const location = useLocation();
  const { appDetail } = useAppDetail();
  useRouteWhitelistGuard(); // 灰度发布：全局路由守卫
  // 管理后台页面（仅 /admin/*），/connect/* 已迁移至应用详情布局下
  const isAdminPage = location.pathname.startsWith('/admin');
  // 应用列表页（首页及灰度新首页）不需要 Sidebar / AppInfoBar
  const isAppListPage = location.pathname === '/' || location.pathname === '/app-list-v2';
  const isDetailPage = !isAdminPage && !isAppListPage;
  
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
      {isDetailPage && <AppInfoBar appDetail={appDetail} style={{ flexShrink: 0 }} />}
      <AntLayout style={{ flex: 1, flexDirection: 'row', overflow: 'hidden' }}>
        {isDetailPage && (
          <Sider 
            width={240}
            className="sidebar"
            style={{ 
              background: '#fff', 
              borderRight: '1px solid #dee0e3', 
              overflowY: 'auto',
              overflowX: 'hidden',
              flexShrink: 0,
              height: sidebarMainHeight,
              padding: 0,
              boxSizing: 'border-box'
            }}
          >
            <Sidebar sidebarMainHeight={sidebarMainHeight} appDetail={appDetail} />
          </Sider>
        )}
        <Content style={{ 
          background: isAdminPage ? '#fff' : '#f5f6f7', 
          padding: isAppListPage ? 0 : (isAdminPage ? 0 : PADDING),
          height: contentHeight,
          overflowY: 'auto',
          overflowX: isAppListPage ? 'auto' : (isAdminPage ? 'auto' : 'hidden'),
          boxSizing: 'border-box'
        }}>
          <div style={undefined}>
            <Outlet />
          </div>
        </Content>
      </AntLayout>
    </AntLayout>
  );
}

export default LayoutInner;
