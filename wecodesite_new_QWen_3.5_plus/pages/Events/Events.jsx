import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Table, Button, Tag, Space, message } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { eventList } from './mock'
import EventDrawer from './EventDrawer'
import styles from './Events.module.less'

const { Column } = Table

function Events() {
  const [searchParams] = useSearchParams()
  const appId = searchParams.get('appId')

  const [loading, setLoading] = useState(false)
  const [eventData] = useState(eventList)
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [editingEvent, setEditingEvent] = useState(null)

  // 打开添加事件 Drawer
  const handleAddEvent = () => {
    setEditingEvent(null)
    setDrawerOpen(true)
  }

  // 打开编辑事件 Drawer
  const handleEditEvent = (record) => {
    setEditingEvent(record)
    setDrawerOpen(true)
  }

  // 删除事件
  const handleDeleteEvent = (record) => {
    // TODO: 实现删除逻辑
    message.success(`已删除事件：${record.name}`)
  }

  // 提交事件配置
  const handleSubmitEvent = (values) => {
    console.log('提交的事件数据:', values)
    // TODO: 更新本地状态或重新加载数据
  }

  // 关闭 Drawer
  const handleCloseDrawer = () => {
    setDrawerOpen(false)
    setEditingEvent(null)
  }

  return (
    <div className={styles.events}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>事件配置</h1>
          <p className={styles.subtitle}>管理和配置应用事件订阅</p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleAddEvent}
        >
          添加事件
        </Button>
      </div>

      <Table
        dataSource={eventData}
        rowKey="id"
        loading={loading}
        pagination={{
          pageSize: 10,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: (total) => `共 ${total} 个事件`,
        }}
      >
        <Column
          title="事件名称"
          dataIndex="name"
          key="name"
          width={200}
        />
        <Column
          title="事件类型"
          dataIndex="type"
          key="type"
          width={150}
        />
        <Column
          title="描述"
          dataIndex="description"
          key="description"
          ellipsis
        />
        <Column
          title="订阅地址"
          dataIndex="subscribeUrl"
          key="subscribeUrl"
          ellipsis
        />
        <Column
          title="状态"
          dataIndex="status"
          key="status"
          width={100}
          render={(status) => (
            <Tag color={status === 'enabled' ? 'green' : 'default'}>
              {status === 'enabled' ? '已启用' : '未启用'}
            </Tag>
          )}
        />
        <Column
          title="创建时间"
          dataIndex="createdAt"
          key="createdAt"
          width={120}
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
                icon={<EditOutlined />}
                onClick={() => handleEditEvent(record)}
              >
                编辑
              </Button>
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
                onClick={() => handleDeleteEvent(record)}
              >
                删除
              </Button>
            </Space>
          )}
        />
      </Table>

      {/* 添加/编辑事件 Drawer */}
      <EventDrawer
        open={drawerOpen}
        onClose={handleCloseDrawer}
        onSubmit={handleSubmitEvent}
        editingEvent={editingEvent}
      />
    </div>
  )
}

export default Events
