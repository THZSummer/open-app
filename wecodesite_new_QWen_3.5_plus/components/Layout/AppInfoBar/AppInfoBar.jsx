import { useNavigate, useSearchParams } from 'react-router-dom'
import { HomeOutlined } from '@ant-design/icons'
import styles from './AppInfoBar.module.less'

function AppInfoBar() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const appId = searchParams.get('appId')

  // TODO: 从 mock 数据中获取应用信息
  const appInfo = {
    name: '测试应用',
    boundEAMAP: 'EAMAP-2024-001',
  }

  const handleHomeClick = () => {
    navigate('/')
  }

  const handleBindClick = () => {
    // TODO: 实现绑定流程
    console.log('触发绑定流程')
  }

  return (
    <div className={styles.appInfoBar}>
      <HomeOutlined className={styles.homeIcon} onClick={handleHomeClick} />
      <div className={styles.info}>
        <span className={styles.appName}>{appInfo.name}</span>
        {appInfo.boundEAMAP ? (
          <span className={styles.boundInfo}>已绑定：{appInfo.boundEAMAP}</span>
        ) : (
          <span className={styles.bindLink} onClick={handleBindClick}>
            立即绑定
          </span>
        )}
      </div>
    </div>
  )
}

export default AppInfoBar
