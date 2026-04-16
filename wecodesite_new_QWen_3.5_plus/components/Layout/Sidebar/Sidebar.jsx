import { useNavigate, useSearchParams, useLocation } from 'react-router-dom'
import styles from './Sidebar.module.less'

const menuGroups = [
  {
    title: '基础信息',
    items: [
      { label: '凭证和基础信息', path: '/basic-info', icon: '🔑' },
      { label: '成员管理', path: '/members', icon: '👥' },
    ],
  },
  {
    title: '应用能力',
    items: [{ label: '添加应用能力', path: '/capabilities', icon: '⚡' }],
  },
  {
    title: '开发配置',
    items: [
      { label: 'API 管理', path: '/api-management', icon: '🔗' },
      { label: '事件配置', path: '/events', icon: '🔔' },
    ],
  },
  {
    title: '版本管理',
    items: [{ label: '版本发布', path: '/version-release', icon: '📦' }],
  },
]

function Sidebar() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const location = useLocation()

  const appId = searchParams.get('appId')
  const caps = searchParams.get('caps')

  const handleMenuClick = (path) => {
    const params = new URLSearchParams()
    if (appId) params.set('appId', appId)
    if (caps && path.includes('capability')) params.set('caps', caps)
    const queryString = params.toString()
    navigate(`${path}${queryString ? `?${queryString}` : ''}`)
  }

  const isActive = (path) => {
    return location.pathname === path
  }

  return (
    <aside className={styles.sidebar}>
      {menuGroups.map((group, groupIndex) => (
        <div key={groupIndex} className={styles.menuGroup}>
          <div className={styles.groupTitle}>{group.title}</div>
          {group.items.map((item, itemIndex) => {
            const active = isActive(item.path)
            return (
              <div
                key={itemIndex}
                className={`${styles.menuItem} ${active ? styles.active : ''}`}
                onClick={() => handleMenuClick(item.path)}
              >
                <span className={styles.menuIcon}>{item.icon}</span>
                <span className={styles.menuLabel}>{item.label}</span>
              </div>
            )
          })}
        </div>
      ))}
    </aside>
  )
}

export default Sidebar
