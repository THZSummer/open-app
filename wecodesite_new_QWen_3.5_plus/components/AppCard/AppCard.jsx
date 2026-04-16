import { useNavigate } from 'react-router-dom'
import styles from './AppCard.module.less'

function AppCard({ app }) {
  const navigate = useNavigate()

  const handleClick = () => {
    navigate(`/basic-info?appId=${app.id}`)
  }

  return (
    <div className={styles.card} onClick={handleClick}>
      <div className={styles.header}>
        <img src={app.icon} alt={app.name} className={styles.icon} />
        <div className={styles.info}>
          <h3 className={styles.title}>{app.name}</h3>
          <span className={`${styles.status} ${styles[app.status]}`}>
            {app.status === 'published' ? '已发布' : '开发中'}
          </span>
        </div>
      </div>
      <div className={styles.meta}>
        <div className={styles.metaItem}>
          <span className={styles.metaLabel}>所有者：</span>
          <span className={styles.metaValue}>{app.owner}</span>
        </div>
        <div className={styles.metaItem}>
          <span className={styles.metaLabel}>我的角色：</span>
          <span className={styles.metaValue}>{app.role}</span>
        </div>
        <div className={styles.metaItem}>
          <span className={styles.metaLabel}>最新动态：</span>
          <span className={styles.metaValue}>{app.latestUpdate}</span>
        </div>
      </div>
      {app.boundEAMAP && (
        <div className={styles.eamap}>
          已绑定：{app.boundEAMAP}
        </div>
      )}
    </div>
  )
}

export default AppCard
