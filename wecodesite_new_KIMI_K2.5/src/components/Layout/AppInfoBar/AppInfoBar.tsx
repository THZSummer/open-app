import React from 'react';
import { useNavigate } from 'react-router-dom';
import { HomeOutlined } from '@ant-design/icons';
import { Card, Button } from 'antd';
import styles from './AppInfoBar.module.less';

interface AppInfoBarProps {
  appName?: string;
  eamapName?: string | null;
  onBindEamap?: () => void;
}

const AppInfoBar: React.FC<AppInfoBarProps> = ({ appName = '应用名称', eamapName, onBindEamap }) => {
  const navigate = useNavigate();

  return (
    <Card className={styles.card} size="small" bordered={false}>
      <div className={styles.container}>
        <HomeOutlined className={styles.homeIcon} onClick={() => navigate('/')} />
        <div className={styles.info}>
          <div className={styles.name}>{appName}</div>
          <div className={styles.meta}>
            {eamapName ? (
              <span>已绑定：{eamapName}</span>
            ) : (
              <>
                <span>未绑定应用服务</span>
                <Button type="link" size="small" onClick={onBindEamap}>[立即绑定]</Button>
              </>
            )}
          </div>
        </div>
      </div>
    </Card>
  );
};

export default AppInfoBar;
