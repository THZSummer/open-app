import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Dropdown, Menu } from 'antd';
import { LogoutOutlined, DownOutlined, ApiOutlined } from '@ant-design/icons';
import LoginModal from './LoginModal';
import { getUserIdCookie, isLoggedIn, removeUserIdCookie } from '../../../utils/cookie';
import { get } from '../../../utils/request';

import './Header.m.less';

function Header() {
  const [loginModalOpen, setLoginModalOpen] = useState(false);
  const [loggedIn, setLoggedIn] = useState(false);
  const [userId, setUserId] = useState('');
  const [userName, setUserName] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    const loggedInStatus = isLoggedIn();
    setLoggedIn(loggedInStatus);
    const id = getUserIdCookie() || '';
    setUserId(id);

    // 已登录时拉取用户姓名
    if (loggedInStatus && id) {
      get('/user-info')
        .then((res) => {
          if (res?.code === '200' && res?.data?.userName) {
            setUserName(res.data.userName);
          }
        })
        .catch(() => {
          // 接口失败不影响页面
        });
    }
  }, []);

  const handleLoginSuccess = (id) => {
    setLoggedIn(true);
    setUserId(id);
    // 登录成功后拉取姓名
    get('/user-info')
      .then((res) => {
        if (res?.code === '200' && res?.data?.userName) {
          setUserName(res.data.userName);
        }
      })
      .catch(() => {});
  };

  const handleLogout = () => {
    removeUserIdCookie();
    setLoggedIn(false);
    setUserId('');
    setUserName('');
    navigate('/');
  };

  const handleApiManagementClick = () => {
    navigate('/api-management');
  };

  const menuItems = loggedIn
    ? [
        { key: 'apiManagement', label: 'API管理', icon: <ApiOutlined />, onClick: handleApiManagementClick },
        { type: 'divider' },
        { key: 'logout', label: '退出登录', icon: <LogoutOutlined />, onClick: handleLogout }
      ]
    : [
        { key: 'login', label: '登录', onClick: () => setLoginModalOpen(true) }
      ];

  /**
   * 渲染用户菜单
   * @returns {React.ReactNode} 用户菜单节点
   */
  const renderUserMenu = () => (
    <Menu>
      {menuItems.map((item) => (
        item.type === 'divider' ? (
          <Menu.Divider key="divider" />
        ) : (
          <Menu.Item key={item.key} icon={item.icon} onClick={item.onClick}>
            {item.label}
          </Menu.Item>
        )
      ))}
    </Menu>
  );

  // 显示名称：优先 userName，否则 userId，最后 "请登录"
  const displayName = loggedIn
    ? (userName || userId || '用户')
    : '请登录';

  return (
    <header className="header">
      <div className="header-left">
        <Link to="/" className="header-logo">
          <div className="logo-icon-box">
            <svg width="12" height="12" viewBox="0 0 16 16" fill="none">
              <path d="M4 8L8 4L12 8L8 12L4 8Z" fill="white"/>
            </svg>
          </div>
          <strong className="logo-text">开放平台</strong>
        </Link>
        <a
          href="https://open.feishu.cn/"
          target="_blank"
          rel="noopener noreferrer"
          className="header-doc-link"
        >
          开发文档
        </a>
      </div>
      <div className="header-right">
        <Dropdown overlay={renderUserMenu()} trigger={['hover', 'click']}>
          <span className="header-user-trigger">
            {displayName}
            {loggedIn && <DownOutlined className="header-user-arrow" />}
          </span>
        </Dropdown>
      </div>
      <LoginModal
        open={loginModalOpen}
        onClose={() => setLoginModalOpen(false)}
        onLoginSuccess={handleLoginSuccess}
      />
    </header>
  );
}

export default Header;
