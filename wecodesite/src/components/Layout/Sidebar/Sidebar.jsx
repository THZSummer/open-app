import React from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  KeyOutlined,
  TeamOutlined,
  ThunderboltOutlined,
  ApiOutlined,
  BellOutlined,
  InboxOutlined,
  RobotOutlined,
  GlobalOutlined,
  AppstoreOutlined,
  MailOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import './Sidebar.m.less';

const iconMap = {
  RobotOutlined: <RobotOutlined />,
  GlobalOutlined: <GlobalOutlined />,
  AppstoreOutlined: <AppstoreOutlined />,
  MailOutlined: <MailOutlined />,
};

const capabilityConfigs = [
  { type: 'bot', name: '机器人', icon: 'RobotOutlined' },
  { type: 'web', name: '网页应用', icon: 'GlobalOutlined' },
  { type: 'miniapp', name: '小程序', icon: 'MailOutlined' },
  { type: 'widget', name: '小组件', icon: 'AppstoreOutlined' },
];

const staticMenuItems = [
  {
    category: '基础信息',
    children: [
      { key: 'basic-info', icon: <KeyOutlined />, label: '凭证和基础信息' },
      { key: 'members', icon: <TeamOutlined />, label: '成员管理' },
    ],
  },
  {
    category: '开发配置',
    children: [
      { key: 'api-management', icon: <ApiOutlined />, label: 'API管理' },
      { key: 'events', icon: <BellOutlined />, label: '事件配置' },
    ],
  },
  {
    category: '版本管理',
    children: [
      { key: 'version-release', icon: <InboxOutlined />, label: '版本发布' },
    ],
  },
];

function Sidebar({ sidebarMainHeight }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId') || '1';
  const enabledCapabilities = searchParams.get('caps')?.split(',').filter(Boolean) || ['bot'];
  const currentPath = window.location.pathname.replace('/', '').split('/')[0] || 'basic-info';

  const isEnabled = (type) => enabledCapabilities.includes(type);

  const handleClick = (key) => {
    navigate(`/${key}?appId=${appId}&caps=${enabledCapabilities.join(',')}`);
  };

  const handleCapabilityClick = (capability) => {
    navigate(`/capability-detail?appId=${appId}&type=${capability.type}&caps=${enabledCapabilities.join(',')}`);
  };

  const getCapabilityMenuItems = () => {
    const items = [];

    items.push({
      key: 'capabilities',
      icon: <ThunderboltOutlined />,
      label: '添加应用能力',
      onClick: () => handleClick('capabilities'),
    });

    capabilityConfigs.forEach((capability) => {
      if (isEnabled(capability.type)) {
        items.push({
          key: `capability-${capability.type}`,
          icon: iconMap[capability.icon] || <ThunderboltOutlined />,
          label: capability.name,
          onClick: () => handleCapabilityClick(capability),
          indent: true,
        });
      }
    });

    return items;
  };

  const capabilityMenuItems = getCapabilityMenuItems();

  return (
    <div className="sidebar-nav">
      {staticMenuItems.map((group, index) => (
        <div key={index} className="sidebar-category">
          <div className="sidebar-category-header">{group.category}</div>
          <ul className="sidebar-category-items">
            {group.children.map((item) => (
              <li
                key={item.key}
                className={`sidebar-item ${currentPath === item.key ? 'active' : ''}`}
                onClick={() => handleClick(item.key)}
              >
                <span className="sidebar-item-icon">{item.icon}</span>
                <span className="sidebar-item-label">{item.label}</span>
              </li>
            ))}
          </ul>
          <div className="sidebar-divider" />
        </div>
      ))}

      <div className="sidebar-category">
        <div className="sidebar-category-header">应用能力</div>
        <ul className="sidebar-category-items">
          {capabilityMenuItems.map((item) => (
            <li
              key={item.key}
              className={`sidebar-item ${currentPath === item.key ? 'active' : ''} ${item.indent ? 'indented' : ''}`}
              onClick={item.onClick}
            >
              <span className="sidebar-item-icon">{item.icon}</span>
              <span className="sidebar-item-label">{item.label}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export default Sidebar;