import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Table, Button, Tag, Space, message } from 'antd'
import { PlusOutlined, EyeOutlined, DeleteOutlined } from '@ant-design/icons'
import { versionList } from './mock'
import styles from './VersionRelease.module.less'

const { Column } = Table

function VersionRelease() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const appId = searchParams.get('appId')

  const [loading, setLoading] = useState(false)
  const [versions] = useState(versionList)

  // 发布新版本
  const handlePublishVersion = () => {
    const params = new URLSearchParams()
    if (appId) params.set('appId', appId)
    navigate(`/version-release/form?${params.toString()}`)
  }

  // 查看版本详情
  const handleViewVersion = (record) => {
    // TODO: 实现查看版本详情
    message.info(`查看版本详情：${record.version}`)
  }

  // 删除版本
  const handleDeleteVersion = (record) => {
    // TODO: 实现删除逻辑
    message.success(`已删除版本：${record.version}`)
  }

  return (
    <div className={styles.versionRelease}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>版本发布</h1>
          <p className={styles.subtitle}>管理和发布应用版本</p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handlePublishVersion}
        >
          发布版本
        </Button>
      </div>

      <Table
        dataSource={versions}
        rowKey="id"
        loading={loading}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 个版本`,
        }}
      >
        <Column
          title="版本号"
          dataIndex="version"
          key="version"
          width={120}
        />
        <Column
          title="版本名称"
          dataIndex="name"
          key="name"
          width={200}
        />
        <Column
          title="描述"
          dataIndex="description"
          key="description"
          ellipsis
        />
        <Column
          title="状态"
          dataIndex="status"
          key="status"
          width={100}
          render={(status) => (
            <Tag color={status === 'published' ? 'green' : 'default'}>
              {status === 'published' ? '已发布' : '草稿'}
            </Tag>
          )}
        />
        <Column
          title="发布时间"
          dataIndex="publishedAt"
          key="publishedAt"
          width={180}
          render={(text) => text || '-'}
        />
        <Column
          title="发布人"
          dataIndex="publishedBy"
          key="publishedBy"
          width={120}
          render={(text) => text || '-'}
        />
        <Column
          title="操作"
          key="action"
          width={180}
          fixed="right"
          render={(_, record) => (
            <Space>
              <Button
                type="link"
                size="small"
                icon={<EyeOutlined />}
                onClick={() => handleViewVersion(record)}
              >
                查看
              </Button>
              {record.status === 'draft' && (
                <Button
                  type="link"
                  size="small"
                  danger
                  icon={<DeleteOutlined />}
                  onClick={() => handleDeleteVersion(record)}
                >
                  删除
                </Button>
              )}
            </Space>
          )}
        />
      </Table>
    </div>
  )
}

export default VersionRelease
