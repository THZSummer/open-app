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
  LineChartOutlined,
  SwapOutlined,
  FolderOutlined,
  CheckCircleOutlined,
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

function Sidebar({ sidebarMainHeight }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId') || '1';
  const enabledCapabilities = searchParams.get('caps')?.split(',').filter(Boolean) || ['bot'];
  const currentPath = window.location.pathname.replace('/', '') || 'basic-info';

  const isEnabled = (type) => enabledCapabilities.includes(type);

  const handleClick = (key) => {
    const path = key.includes('/') ? key : `/${key}`;
    navigate(`${path}?appId=${appId}&caps=${enabledCapabilities.join(',')}`);
  };

  const handleCapabilityClick = (capability) => {
    navigate(`/capability-detail?appId=${appId}&type=${capability.type}&caps=${enabledCapabilities.join(',')}`);
  };

  const getCapabilityChildren = () => {
    const children = [
      { key: 'capabilities', icon: <ThunderboltOutlined />, label: '添加应用能力' },
    ];

    capabilityConfigs.forEach((capability) => {
      if (isEnabled(capability.type)) {
        children.push({
          key: `capability-${capability.type}`,
          icon: iconMap[capability.icon] || <ThunderboltOutlined />,
          label: capability.name,
          capability: capability,
          indent: true,
        });
      }
    });

    return children;
  };

  const capabilityChildren = getCapabilityChildren();

  const dynamicMenuItems = [
    {
      category: '基础信息',
      children: [
        { key: 'basic-info', icon: <KeyOutlined />, label: '凭证和基础信息' },
        { key: 'members', icon: <TeamOutlined />, label: '成员管理' },
      ],
    },
    {
      category: '应用能力',
      children: capabilityChildren,
    },
    {
      category: '开发配置',
      children: [
        { key: 'api-management', icon: <ApiOutlined />, label: 'API管理' },
        { key: 'events', icon: <BellOutlined />, label: '事件配置' },
        { key: 'callbacks', icon: <SwapOutlined />, label: '回调配置' },
      ],
    },
    {
      category: '运营监控',
      children: [
        { key: 'operation-log', icon: <LineChartOutlined />, label: '操作日志' },
      ],
    },
    {
      category: '版本管理',
      children: [
        { key: 'version-release', icon: <InboxOutlined />, label: '版本发布' },
      ],
    },
    {
      category: '后台管理',
      children: [
        { key: 'admin/categories', icon: <FolderOutlined />, label: '分类管理' },
        { key: 'admin/apis', icon: <ApiOutlined />, label: 'API管理' },
        { key: 'admin/events', icon: <BellOutlined />, label: '事件管理' },
        { key: 'admin/callbacks', icon: <SwapOutlined />, label: '回调管理' },
        { key: 'admin/approvals', icon: <CheckCircleOutlined />, label: '审批中心' },
      ],
    },
  ];

  return (
    <div className="sidebar" style={{ height: sidebarMainHeight || '100%', overflowY: 'auto' }}>
      <div className="sidebar-nav">
        {dynamicMenuItems.map((group, index) => (
          <div key={index} className="sidebar-category">
            <div className="sidebar-category-header">{group.category}</div>
            <ul className="sidebar-category-items">
              {group.children.map((item) => (
                <li
                  key={item.key}
                  className={`sidebar-item ${currentPath === item.key || currentPath.startsWith(item.key + '/') ? 'active' : ''} ${item.indent ? 'indented' : ''}`}
                  onClick={() => item.capability ? handleCapabilityClick(item.capability) : handleClick(item.key)}
                >
                  <span className="sidebar-item-icon">{item.icon}</span>
                  <span className="sidebar-item-label">{item.label}</span>
                </li>
              ))}
            </ul>
            <div className="sidebar-divider" />
          </div>
        ))}
      </div>
    </div>
  );
}

export default Sidebar;