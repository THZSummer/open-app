import React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Header from './Header/Header';
import AppInfoBar from './AppInfoBar/AppInfoBar';
import Sidebar from './Sidebar/Sidebar';
import styles from './Layout.module.less';

interface LayoutProps {
  showAppInfo?: boolean;
  appName?: string;
  eamapName?: string | null;
  onBindEamap?: () => void;
}

const Layout: React.FC<LayoutProps> = ({ showAppInfo = false, appName, eamapName, onBindEamap }) => {
  const location = useLocation();
  const isHome = location.pathname === '/';

  return (
    <div className={styles.layout}>
      <Header />
      {showAppInfo && <AppInfoBar appName={appName} eamapName={eamapName} onBindEamap={onBindEamap} />}
      <div className={styles.main}>
        {!isHome && <Sidebar />}
        <main className={`${styles.content} ${isHome ? styles.fullWidth : ''}`}>
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default Layout;
