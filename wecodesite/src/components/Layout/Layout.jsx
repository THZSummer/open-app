import React, { useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Layout as AntLayout } from 'antd';
import { loadMicroApp } from 'qiankun';
import { useRouteWhitelistGuard } from '../../hooks/useRouteWhitelistGuard';
import microApps from '../../microApps';
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
  useRouteWhitelistGuard(); // 灰度发布：全局路由守卫

  const isAdminPage = location.pathname.startsWith('/admin');
  const isAppListPage = location.pathname === '/appList' || location.pathname === '/app-list-v2';
  // 判断是否为微前端子应用页面（qiankun 子应用统一以 /qiankun/ 为前缀）
  const isMicroAppPage = location.pathname.startsWith('/qiankun/');
  const isDetailPage = !isAdminPage && !isAppListPage;

  // 根据当前路由匹配对应的微前端子应用名称
  const currentMicroAppName = isMicroAppPage
    ? microApps.find((app) => location.pathname.startsWith('/' + app.menuKey))?.name
    : null;

  // 手动加载/卸载微前端子应用（兼容 hash 路由模式）
  useEffect(() => {
    if (!currentMicroAppName) return;

    const matchedApp = microApps.find((app) => app.name === currentMicroAppName);
    if (!matchedApp) return;

    // loadMicroApp 返回 MicroApp 句柄对象（非 Promise），包含 mount/unmount 方法
    const handle = loadMicroApp(
      {
        name: matchedApp.name,
        entry: matchedApp.entry,
        container: '#sub-app-viewport',
      },
      {
        sandbox: { experimentalStyleIsolation: true },
      }
    );

    // 清理函数：离开子应用路由时卸载子应用
    return () => {
      handle.unmount();
    };
  }, [currentMicroAppName]);

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
            <Sidebar sidebarMainHeight={sidebarMainHeight} />
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
          {/* 微前端子应用页面：隐藏 Outlet，显示 qiankun 挂载容器 */}
          {isMicroAppPage ? (
            <div id="sub-app-viewport" style={{ minHeight: '100%' }} />
          ) : (
            <div style={undefined}>
              <Outlet />
            </div>
          )}
        </Content>
      </AntLayout>
    </AntLayout>
  );
}

export default LayoutInner;
