import React from 'react';
import { Tag } from 'antd';
import { useNavigate } from 'react-router-dom';
import type { App } from '../../types';
import styles from './AppCard.module.less';

interface AppCardProps {
  app: App;
}

const AppCard: React.FC<AppCardProps> = ({ app }) => {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate(`/basic-info?appId=${app.id}`);
  };

  return (
    <div className={styles.card} onClick={handleClick}>
      <div className={styles.header}>
        <div className={styles.icon}>{app.icon}</div>
        <div className={styles.info}>
          <div className={styles.name}>{app.name}</div>
          <Tag color={app.eamap ? 'success' : 'warning'}>{app.eamap ? '已绑定' : '未绑定'}</Tag>
        </div>
      </div>
      <div className={styles.body}>
        <div className={styles.row}><span className={styles.label}>所有者：</span><span className={styles.value}>{app.owner}</span></div>
        <div className={styles.row}><span className={styles.label}>我的角色：</span><span className={styles.value}>{app.role}</span></div>
        <div className={styles.row}><span className={styles.label}>最新动态：</span><span className={styles.value}>{app.updateTime}</span></div>
      </div>
    </div>
  );
};

export default AppCard;
