import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ADMIN_MENU_CONFIG } from '@/utils/constants';
import { isInAdminWhitelist } from '@/utils/common';
import './SimpleSidebar.m.less';

function SimpleSidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const currentPath = location.pathname;
  const [isWhitelisted, setIsWhitelisted] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const checkWhitelist = async () => {
      try {
        const result = await isInAdminWhitelist();
        setIsWhitelisted(result);
      } catch (error) {
        console.error('Failed to check whitelist:', error);
        setIsWhitelisted(false);
      } finally {
        setLoading(false);
      }
    };

    checkWhitelist();
  }, []);

  const isActive = (router) => {
    return currentPath === router || currentPath.startsWith(router + '/');
  };

  const handleClick = (router) => {
    navigate(router);
  };

  if (loading) {
    return (
      <div className="simple-sidebar">
        <div className="simple-sidebar-header">管理后台</div>
        <div style={{ padding: '16px', textAlign: 'center', color: '#999' }}>
          加载中...
        </div>
      </div>
    );
  }

  const visibleMenus = isWhitelisted 
    ? ADMIN_MENU_CONFIG 
    : ADMIN_MENU_CONFIG.filter(item => item.router === '/admin/approvals');

  return (
    <div className="simple-sidebar">
      <div className="simple-sidebar-header">管理后台</div>
      <ul className="simple-sidebar-items">
        {visibleMenus.map((item) => (
          <li
            key={item.router}
            className={`simple-sidebar-item ${isActive(item.router) ? 'active' : ''}`}
            onClick={() => handleClick(item.router)}
          >
            {item.title}
          </li>
        ))}
      </ul>
    </div>
  );
}

export default SimpleSidebar;
