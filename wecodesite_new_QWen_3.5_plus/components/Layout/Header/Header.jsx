import { useNavigate } from 'react-router-dom'
import { HomeOutlined } from '@ant-design/icons'
import styles from './Header.module.less'

function Header() {
  const navigate = useNavigate()

  const handleLogoClick = () => {
    navigate('/')
  }

  const handleDocsClick = () => {
    window.open('https://open.feishu.cn/', '_blank')
  }

  return (
    <header className={styles.header}>
      <div className={styles.logo} onClick={handleLogoClick}>
        <div className={styles.logoIcon}>🔷</div>
        <span className={styles.logoText}>开放平台</span>
      </div>
      <nav className={styles.nav}>
        <a
          href="#"
          className={styles.navLink}
          onClick={(e) => {
            e.preventDefault()
            handleDocsClick()
          }}
        >
          开发文档
        </a>
        <span className={styles.navLink}>开发者</span>
      </nav>
    </header>
  )
}

export default Header
