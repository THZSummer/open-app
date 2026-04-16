import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Card, Button, Tag, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { capabilities } from './mock'
import styles from './Capabilities.module.less'

function Capabilities() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const appId = searchParams.get('appId')

  const [capabilityList] = useState(capabilities)

  const handleAddCapability = () => {
    // TODO: 实现添加能力流程
    message.info('添加应用能力功能开发中')
  }

  const handleCapabilityClick = (type) => {
    const params = new URLSearchParams()
    if (appId) params.set('appId', appId)
    params.set('type', type)
    navigate(`/capability-detail?${params.toString()}`)
  }

  return (
    <div className={styles.capabilities}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>添加应用能力</h1>
          <p className={styles.subtitle}>选择并配置应用需要的能力</p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleAddCapability}
        >
          添加能力
        </Button>
      </div>

      <div className={styles.grid}>
        {capabilityList.map((cap) => (
          <Card
            key={cap.id}
            className={styles.card}
            hoverable
            onClick={() => handleCapabilityClick(cap.type)}
          >
            <div className={styles.icon}>{cap.icon}</div>
            <h3 className={styles.name}>{cap.name}</h3>
            <p className={styles.description}>{cap.description}</p>
            <div className={styles.footer}>
              <Tag color={cap.status === 'enabled' ? 'green' : 'default'}>
                {cap.status === 'enabled' ? '已启用' : '未启用'}
              </Tag>
            </div>
          </Card>
        ))}
      </div>
    </div>
  )
}

export default Capabilities
