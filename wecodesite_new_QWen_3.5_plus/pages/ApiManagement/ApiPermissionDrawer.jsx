import { useState } from 'react'
import { Drawer, Button, Table, Checkbox, message, Space, Tag } from 'antd'
import { apiList } from './mock'
import styles from './ApiPermissionDrawer.module.less'

const { Column } = Table

function ApiPermissionDrawer({ open, onClose, onSubmit }) {
  const [loading, setLoading] = useState(false)
  const [selectedApiIds, setSelectedApiIds] = useState([])

  // 处理 API 选择
  const handleApiSelect = (selectedIds) => {
    setSelectedApiIds(selectedIds)
  }

  // 提交开通申请
  const handleSubmit = async () => {
    if (selectedApiIds.length === 0) {
      message.warning('请至少选择一个 API')
      return
    }

    try {
      setLoading(true)
      // TODO: 调用 API 开通权限
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      message.success(`成功申请 ${selectedApiIds.length} 个 API 的权限`)
      setSelectedApiIds([])
      onSubmit?.(selectedApiIds)
      onClose()
    } catch (error) {
      message.error('申请失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  // 取消操作
  const handleCancel = () => {
    setSelectedApiIds([])
    onClose()
  }

  // 筛选未授权和已授权的 API
  const availableApis = apiList.filter(api => api.permissionStatus === 'not_granted')

  return (
    <Drawer
      title="开通 API 权限"
      placement="right"
      width={800}
      open={open}
      onClose={handleCancel}
      footer={
        <Space>
          <Button onClick={handleCancel}>取消</Button>
          <Button
            type="primary"
            loading={loading}
            onClick={handleSubmit}
            disabled={selectedApiIds.length === 0}
          >
            批量开通 ({selectedApiIds.length})
          </Button>
        </Space>
      }
    >
      <div className={styles.drawerContent}>
        <div className={styles.tip}>
          请选择需要开通的 API 权限，支持多选
        </div>
        <Table
          dataSource={availableApis}
          rowKey="id"
          pagination={false}
          scroll={{ y: 500 }}
          rowSelection={{
            type: 'checkbox',
            selectedRowKeys: selectedApiIds,
            onChange: handleApiSelect,
          }}
        >
          <Column
            title="API 名称"
            dataIndex="name"
            key="name"
            width={200}
          />
          <Column
            title="请求方式"
            dataIndex="method"
            key="method"
            width={100}
            render={(method) => (
              <Tag color={
                method === 'GET' ? 'blue' :
                method === 'POST' ? 'green' :
                method === 'PUT' ? 'orange' :
                'red'
              }>
                {method}
              </Tag>
            )}
          />
          <Column
            title="路径"
            dataIndex="path"
            key="path"
            ellipsis
          />
          <Column
            title="描述"
            dataIndex="description"
            key="description"
            ellipsis
          />
        </Table>
      </div>
    </Drawer>
  )
}

export default ApiPermissionDrawer
