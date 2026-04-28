import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { ADMIN_MENU_CONFIG } from '@/utils/constants';
import './SimpleSidebar.m.less';

function SimpleSidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const currentPath = location.pathname;

  const isActive = (router) => {
    return currentPath === router || currentPath.startsWith(router + '/');
  };

  const handleClick = (router) => {
    navigate(router);
  };

  return (
    <div className="simple-sidebar">
      <div className="simple-sidebar-header">管理后台</div>
      <ul className="simple-sidebar-items">
        {ADMIN_MENU_CONFIG.map((item) => (
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
