import React from 'react';
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import styles from './Sidebar.module.less';

const Sidebar: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const appId = searchParams.get('appId');
  const caps = searchParams.get('caps') || '';
  const enabledCaps = caps.split(',').filter(Boolean);

  const menuGroups = [
    {
      title: '基础信息',
      items: [
        { key: '/basic-info', label: '凭证和基础信息', path: '/basic-info' },
        { key: '/members', label: '成员管理', path: '/members' },
      ],
    },
    {
      title: '应用能力',
      items: [
        { key: '/capabilities', label: '添加应用能力', path: '/capabilities' },
        ...(enabledCaps.includes('bot') ? [{ key: '/capability-detail?type=bot', label: '机器人', path: '/capability-detail?type=bot', indent: true }] : []),
        ...(enabledCaps.includes('web') ? [{ key: '/capability-detail?type=web', label: '网页应用', path: '/capability-detail?type=web', indent: true }] : []),
      ],
    },
    {
      title: '开发配置',
      items: [
        { key: '/api-management', label: 'API管理', path: '/api-management' },
        { key: '/events', label: '事件配置', path: '/events' },
      ],
    },
    {
      title: '运营监控',
      items: [{ key: '/operation-log', label: '操作日志', path: '/operation-log' }],
    },
    {
      title: '版本管理',
      items: [{ key: '/version-release', label: '版本发布', path: '/version-release' }],
    },
  ];

  const handleClick = (path: string) => {
    const separator = path.includes('?') ? '&' : '?';
    navigate(`${path}${separator}appId=${appId}&caps=${caps}`);
  };

  return (
    <aside className={styles.sidebar}>
      {menuGroups.map((group) => (
        <div key={group.title} className={styles.group}>
          <div className={styles.groupTitle}>{group.title}</div>
          {group.items.map((item: any) => {
            const isActive = location.pathname === item.path || (item.path.includes('?') && location.pathname === item.path.split('?')[0]);
            return (
              <div
                key={item.key}
                className={`${styles.menuItem} ${item.indent ? styles.indent : ''} ${isActive ? styles.active : ''}`}
                onClick={() => handleClick(item.path)}
              >
                {item.label}
              </div>
            );
          })}
        </div>
      ))}
    </aside>
  );
};

export default Sidebar;
