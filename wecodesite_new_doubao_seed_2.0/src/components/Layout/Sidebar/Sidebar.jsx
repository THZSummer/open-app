import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import './Sidebar.m.less';

const Sidebar = () => {
  const location = useLocation();
  const searchParams = new URLSearchParams(location.search);
  const appId = searchParams.get('appId');
  const caps = searchParams.get('caps') || '';

  // 菜单项配置
  const menuItems = [
    {
      title: '基础信息',
      items: [
        {
          name: '凭证和基础信息',
          path: '/basic-info',
          key: 'basic-info'
        },
        {
          name: '成员管理',
          path: '/members',
          key: 'members'
        }
      ]
    },
    {
      title: '应用能力',
      items: [
        {
          name: '添加应用能力',
          path: '/capabilities',
          key: 'capabilities'
        }
      ]
    },
    {
      title: '开发配置',
      items: [
        {
          name: 'API管理',
          path: '/api-management',
          key: 'api-management'
        },
        {
          name: '事件配置',
          path: '/events',
          key: 'events'
        }
      ]
    },
    {
      title: '版本管理',
      items: [
        {
          name: '版本发布',
          path: '/version-release',
          key: 'version-release'
        }
      ]
    }
  ];

  // 生成带参数的链接
  const generateLink = (path) => {
    const params = new URLSearchParams();
    if (appId) params.append('appId', appId);
    if (caps) params.append('caps', caps);
    const search = params.toString() ? `?${params.toString()}` : '';
    return `${path}${search}`;
  };

  // 检查当前路径是否激活
  const isActive = (path) => {
    return location.pathname === path;
  };

  return (
    <div className="sidebar">
      {menuItems.map((category, index) => (
        <div key={index} className="category">
          <div className="categoryTitle">{category.title}</div>
          <div className="menuItems">
            {category.items.map((item) => (
              <Link
                key={item.key}
                to={generateLink(item.path)}
                className={`menuItem ${isActive(item.path) ? 'active' : ''}`}
              >
                {item.name}
              </Link>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
};

export default Sidebar;