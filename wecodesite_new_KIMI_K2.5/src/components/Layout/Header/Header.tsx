import React from 'react';
import { useNavigate } from 'react-router-dom';
import styles from './Header.module.less';

const Header: React.FC = () => {
  const navigate = useNavigate();

  return (
    <header className={styles.header}>
      <div className={styles.left}>
        <div className={styles.logo} onClick={() => navigate('/')}>
          <span className={styles.logoIcon} />
        </div>
        <span className={styles.brand}>开放平台</span>
      </div>
      <div className={styles.right}>
        <a href="https://open.feishu.cn/" target="_blank" rel="noopener noreferrer" className={styles.link}>
          开发文档
        </a>
        <span className={styles.user}>开发者</span>
      </div>
    </header>
  );
};

export default Header;
