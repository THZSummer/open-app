import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Layout, Dropdown } from 'antd';
import { LoginOutlined, LogoutOutlined } from '@ant-design/icons';
import LoginModal from './LoginModal';
import { getUserIdCookie, isLoggedIn, removeUserIdCookie } from '../../../utils/cookie';

const { Header: AntHeader } = Layout;

function Header() {
  const [loginModalOpen, setLoginModalOpen] = useState(false);
  const [loggedIn, setLoggedIn] = useState(false);
  const [userId, setUserId] = useState('');

  useEffect(() => {
    setLoggedIn(isLoggedIn());
    setUserId(getUserIdCookie() || '');
  }, []);

  const handleLoginSuccess = (id) => {
    setLoggedIn(true);
    setUserId(id);
  };

  const handleLogout = () => {
    removeUserIdCookie();
    setLoggedIn(false);
    setUserId('');
  };

  const menuItems = loggedIn
    ? [
        { key: 'user_id', label: `用户ID: ${userId}`, disabled: true },
        { type: 'divider' },
        { key: 'logout', label: '退出登录', icon: <LogoutOutlined />, onClick: handleLogout }
      ]
    : [
        { key: 'login', label: '登录', icon: <LoginOutlined />, onClick: () => setLoginModalOpen(true) }
      ];

  return (
    <AntHeader style={{ 
      background: '#fff', 
      padding: '0 24px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      borderBottom: '1px solid #f0f0f0'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 40 }}>
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <div style={{
            width: 32,
            height: 32,
            borderRadius: 6,
            background: '#0066ff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M4 8L8 4L12 8L8 12L4 8Z" fill="white"/>
            </svg>
          </div>
          <strong style={{ fontSize: 16, color: '#1f1f1f' }}>开放平台</strong>
        </Link>
        <a 
          href="https://open.feishu.cn/"
          target="_blank"
          rel="noopener noreferrer"
          style={{ color: '#5e5e5e' }}
        >
          开发文档
        </a>
      </div>
      <Dropdown menu={{ items: menuItems }} trigger={['hover', 'click']}>
        <span style={{ color: '#8c8c8c', cursor: 'pointer' }}>开发者</span>
      </Dropdown>
      <LoginModal 
        open={loginModalOpen} 
        onClose={() => setLoginModalOpen(false)} 
        onLoginSuccess={handleLoginSuccess} 
      />
    </AntHeader>
  );
}

export default Header;