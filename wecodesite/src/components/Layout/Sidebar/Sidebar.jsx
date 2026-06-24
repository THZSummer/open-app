import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams, useLocation } from 'react-router-dom';
import {
  KeyOutlined,
  TeamOutlined,
  ThunderboltOutlined,
  ApiOutlined,
  BellOutlined,
  InboxOutlined,
  LineChartOutlined,
  SwapOutlined,
  MonitorOutlined,
} from '@ant-design/icons';
import * as Icons from '@ant-design/icons';
import { fetchSubscribedAbilities } from '../../../pages/Capabilities/thunk';
import './Sidebar.m.less';

/**
 * 根据后端返回的 icon 名称动态渲染 Ant Design Icon 组件
 */
function DynamicIcon({ iconName, iconUrl, size = 14 }) {
  if (iconUrl) {
    return <img src={iconUrl} alt="" style={{ width: size, height: size, objectFit: 'contain' }} />;
  }
  if (!iconName) return <ThunderboltOutlined style={{ fontSize: size }} />;
  const IconComp = Icons[iconName];
  if (!IconComp) return <ThunderboltOutlined style={{ fontSize: size }} />;
  return <IconComp style={{ fontSize: size }} />;
}

// 自定义事件：能力变更时通知 Sidebar 刷新
const ABILITY_CHANGED_EVENT = 'app:ability-changed';

function Sidebar({ sidebarMainHeight, appDetail }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const location = useLocation();
  const appId = searchParams.get('appId') || '1';
  const currentPath = window.location.pathname.replace('/', '') || 'basic-info';
  const currentSub = searchParams.get('sub') || '';

  // 动态已订阅能力列表
  const [subscribedAbilities, setSubscribedAbilities] = useState([]);
  // 从 appDetail prop 获取应用类型
  const appType = appDetail?.appType ?? null;
  const appSubType = appDetail?.appSubType ?? null;

  const loadSubscribedAbilities = useCallback(async () => {
    if (!appId) return;
    try {
      const res = await fetchSubscribedAbilities(appId);
      if (res?.code === '200' && Array.isArray(res.data)) {
        setSubscribedAbilities(res.data.filter(a => a.abilityType !== 6));
      }
    } catch (e) {
      console.error('Failed to load subscribed abilities for sidebar', e);
    }
  }, [appId]);

  // appType 变化后，业务应用才请求 abilities
  useEffect(() => {
    if (appType != null && appType !== 0) {
      loadSubscribedAbilities();
    }
  }, [appType, loadSubscribedAbilities]);

  // 监听能力变更自定义事件
  useEffect(() => {
    const handleAbilityChanged = () => {
      loadSubscribedAbilities();
    };
    window.addEventListener(ABILITY_CHANGED_EVENT, handleAbilityChanged);
    return () => {
      window.removeEventListener(ABILITY_CHANGED_EVENT, handleAbilityChanged);
    };
  }, [loadSubscribedAbilities]);

  const handleClick = (key) => {
    // key 可能形如 'basic-info' 或 'connect/connectors'，统一补齐前导 '/'
    const path = key.startsWith('/') ? key : `/${key}`;
    navigate(`${path}?appId=${appId}`);
  };

  const handleSubscribedAbilityClick = (ability) => {
    navigate(`/capabilities?appId=${appId}&sub=${ability.abilityType}`);
  };

  // 构建"应用能力"分组的子菜单
  const getCapabilityChildren = () => {
    const children = [
      { key: 'capabilities', icon: <ThunderboltOutlined />, label: '添加应用能力' },
    ];
    subscribedAbilities.forEach((ability) => {
      children.push({
        key: `capability-${ability.abilityType}`,
        icon: <DynamicIcon iconName={ability.icon} iconUrl={ability.iconUrl} />,
        label: ability.nameCn,
        ability: ability,
      });
    });
    return children;
  };

  const capabilityChildren = getCapabilityChildren();

  const dynamicMenuItems = [
    {
      category: '基础信息',
      children: [
        { key: 'basic-info', icon: <KeyOutlined />, label: '凭证和基础信息' },
        ...(appType !== 0 ? [{ key: 'members', icon: <TeamOutlined />, label: '成员管理' }] : []),
      ],
    },
    // 业务应用：显示完整菜单
    ...(appType !== 0 ? [
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
        category: '连接平台',
        children: [
          { key: 'connect/connectors', icon: <ApiOutlined />, label: '连接器' },
          { key: 'connect/flows', icon: <SwapOutlined />, label: '连接流' },
        ],
      },
      {
        category: '运营监控',
        children: [
          { key: 'run-management', icon: <MonitorOutlined />, label: '运行管理' },
          { key: 'operation-log', icon: <LineChartOutlined />, label: '操作日志' },
        ],
      },
      {
        category: '版本管理',
        children: [
          { key: 'version-release', icon: <InboxOutlined />, label: '版本发布与审核' },
        ],
      },
    ] : []),
    // 个人应用：仅显示 API管理
    ...(appType === 0 ? [
      {
        category: '开发配置',
        children: [
          { key: 'api-management', icon: <ApiOutlined />, label: 'API管理' },
        ],
      },
    ] : []),
  ];

  return (
    <div className="sidebar" style={{ overflowY: 'auto' }}>
      <div className="sidebar-nav">
        {dynamicMenuItems.map((group, index) => (
          <div key={index} className="sidebar-category">
            <div className="sidebar-category-header">{group.category}</div>
            <ul className="sidebar-category-items">
              {group.children.map((item) => (
                <li
                  key={item.key}
                  className={`sidebar-item ${
                    item.ability
                      ? (currentPath === 'capabilities' && currentSub === String(item.ability.abilityType) ? 'active' : '')
                      : item.key === 'capabilities'
                        ? (currentPath === 'capabilities' && (!currentSub || currentSub === 'add') ? 'active' : '')
                        : (currentPath === item.key || currentPath.startsWith(item.key + '/') ? 'active' : '')
                  }`}
                  onClick={() => item.ability ? handleSubscribedAbilityClick(item.ability) : handleClick(item.key)}
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

// 导出自定义事件名，供其他组件使用
export { ABILITY_CHANGED_EVENT };
export default Sidebar;
