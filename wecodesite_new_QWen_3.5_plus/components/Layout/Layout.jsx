import { useLocation, Outlet } from 'react-router-dom'
import Header from './Header/Header'
import AppInfoBar from './AppInfoBar/AppInfoBar'
import Sidebar from './Sidebar/Sidebar'
import styles from './Layout.module.less'

const HEADER_HEIGHT = 64
const APP_INFO_BAR_HEIGHT = 50
const PADDING = 24

function Layout() {
  const location = useLocation()
  const isHomePage = location.pathname === '/'

  const contentAreaHeight = isHomePage
    ? `calc(100vh - ${HEADER_HEIGHT}px - ${PADDING}px)`
    : `calc(100vh - ${HEADER_HEIGHT}px - ${APP_INFO_BAR_HEIGHT}px - ${PADDING}px)`

  return (
    <div className={styles.layout}>
      <Header />
      {!isHomePage && <AppInfoBar />}
      <div className={styles.main}>
        {!isHomePage && <Sidebar />}
        <main
          className={styles.content}
          style={{ height: contentAreaHeight }}
        >
          <Outlet />
        </main>
      </div>
    </div>
  )
}

export default Layout
