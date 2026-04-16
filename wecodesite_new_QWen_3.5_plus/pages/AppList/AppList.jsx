import { useState } from 'react'
import { PlusOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import AppCard from '../../components/AppCard/AppCard'
import { apps } from './mock'
import styles from './AppList.module.less'

function AppList() {
  const [appList] = useState(apps)

  const handleCreateApp = () => {
    // TODO: 实现创建应用流程
    console.log('创建应用')
  }

  return (
    <div className={styles.appList}>
      <div className={styles.header}>
        <h1 className={styles.title}>我的应用</h1>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleCreateApp}
        >
          创建应用
        </Button>
      </div>
      <div className={styles.grid}>
        {appList.map((app) => (
          <AppCard key={app.id} app={app} />
        ))}
      </div>
    </div>
  )
}

export default AppList
