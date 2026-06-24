import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';
import { fetchAppById } from '../pages/AppList/thunk';

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const location = useLocation();
  const [searchParams] = useSearchParams();
  // 仅 /admin/* 属于管理后台页面；/connect/* 属于应用详情下的功能页，需正常加载 appDetail
  const isAdminPage = location.pathname.startsWith('/admin');
  const isDetailPage = location.pathname !== '/' && !isAdminPage;
  const appId = searchParams.get('appId') || '';

  const [appDetail, setAppDetail] = useState(null);
  const [appDetailLoading, setAppDetailLoading] = useState(false);

  useEffect(() => {
    if (isDetailPage && appId) {
      setAppDetailLoading(true);
      fetchAppById(appId)
        .then((result) => {
          if (result?.code === '200' && result.data) {
            setAppDetail(result.data);
          } else {
            setAppDetail(null);
          }
        })
        .catch(() => setAppDetail(null))
        .finally(() => setAppDetailLoading(false));
    } else {
      setAppDetail(null);
      setAppDetailLoading(false);
    }
  }, [appId, isDetailPage]);

  const reloadAppDetail = useCallback(() => {
    if (appId) {
      return fetchAppById(appId).then((result) => {
        if (result?.code === '200' && result.data) {
          setAppDetail(result.data);
          return result.data;
        }
        return null;
      });
    }
    return Promise.resolve(null);
  }, [appId]);

  return (
    <AppContext.Provider value={{ appDetail, appDetailLoading, appId, reloadAppDetail }}>
      {children}
    </AppContext.Provider>
  );
}

export function useAppDetail() {
  const ctx = useContext(AppContext);
  if (!ctx) {
    throw new Error('useAppDetail must be used within AppProvider');
  }
  return ctx;
}

export default AppContext;
