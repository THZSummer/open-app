import { useState, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Table, Button, Select, Space, Tag, message } from 'antd'
import { PlusOutlined, EditOutlined } from '@ant-design/icons'
import { apiList, apiCategories } from './mock'
import ApiPermissionDrawer from './ApiPermissionDrawer'
import styles from './ApiManagement.module.less'

const { Option } = Select
const { Column } = Table

function ApiManagement() {
  const [searchParams] = useSearchParams()
  const appId = searchParams.get('appId')

  const [loading, setLoading] = useState(false)
  const [category, setCategory] = useState('all')
  const [pageSize, setPageSize] = useState(10)
  const [currentPage, setCurrentPage] = useState(1)
  const [drawerOpen, setDrawerOpen] = useState(false)

  // 根据分类筛选 API
  const filteredApis = useMemo(() => {
    if (category === 'all') {
      return apiList
    }
    return apiList.filter(api => api.category === category)
  }, [category])

  // 模拟后端分页
  const paginatedApis = useMemo(() => {
    const start = (currentPage - 1) * pageSize
    const end = start + pageSize
    return filteredApis.slice(start, end)
  }, [filteredApis, currentPage, pageSize])

  // 处理开通权限
  const handleOpenDrawer = () => {
    setDrawerOpen(true)
  }

  // 提交开通申请
  const handleSubmitPermission = (apiIds) => {
    console.log('开通的 API IDs:', apiIds)
    // TODO: 更新本地状态或重新加载数据
  }

  // 表格列定义
  const columns = [
    {
      title: 'API 名称',
      dataIndex: 'name',
      key: 'name',
      fixed: 'left',
      width: 200,
    },
    {
      title: '请求方式',
      dataIndex: 'method',
      key: 'method',
      width: 100,
      render: (method) => {
        const colorMap = {
          GET: 'blue',
          POST: 'green',
          PUT: 'orange',
          DELETE: 'red',
        }
        return (
          <Tag color={colorMap[method] || 'default'}>
            {method}
          </Tag>
        )
      },
    },
    {
      title: '路径',
      dataIndex: 'path',
      key: 'path',
      ellipsis: true,
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      width: 120,
      render: (category) => {
        const categoryMap = {
          user: '用户管理',
          message: '消息推送',
          calendar: '日历会议',
          drive: '云文档',
          approval: '审批',
          contact: '通讯录',
        }
        return <span>{categoryMap[category] || category}</span>
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
    {
      title: '权限状态',
      dataIndex: 'permissionStatus',
      key: 'permissionStatus',
      width: 120,
      render: (status) => (
        <Tag color={status === 'granted' ? 'green' : 'default'}>
          {status === 'granted' ? '已授权' : '未授权'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => {
              message.info('查看 API 详情功能开发中')
            }}
          >
            查看
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div className={styles.apiManagement}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>API 管理</h1>
          <p className={styles.subtitle}>管理和配置应用 API 权限</p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleOpenDrawer}
        >
          开通权限
        </Button>
      </div>

      <div className={styles.filters}>
        <Space>
          <span className={styles.filterLabel}>分类：</span>
          <Select
            value={category}
            onChange={setCategory}
            style={{ width: 150 }}
          >
            {apiCategories.map((cat) => (
              <Option key={cat.value} value={cat.value}>
                {cat.label}
              </Option>
            ))}
          </Select>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={paginatedApis}
        rowKey="id"
        loading={loading}
        scroll={{ x: 1200 }}
        pagination={{
          current: currentPage,
          pageSize: pageSize,
          total: filteredApis.length,
          showSizeChanger: true,
          showQuickJumper: true,
          pageSizeOptions: ['10', '20', '50'],
          showTotal: (total) => `共 ${total} 个 API`,
          onChange: (page, size) => {
            setCurrentPage(page)
            setPageSize(size)
          },
        }}
      />

      {/* 开通权限 Drawer */}
      <ApiPermissionDrawer
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onSubmit={handleSubmitPermission}
      />
    </div>
  )
}

export default ApiManagement
