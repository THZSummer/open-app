import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Table, Button, Modal, Form, Input, Select, message, Space, Tag, Avatar } from 'antd'
import { PlusOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons'
import { members, roleOptions } from './mock'
import styles from './Members.module.less'

const { Option } = Select

function Members() {
  const [searchParams] = useSearchParams()
  const appId = searchParams.get('appId')

  const [loading, setLoading] = useState(false)
  const [memberList] = useState(members)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingMember, setEditingMember] = useState(null)
  const [form] = Form.useForm()

  // 打开添加成员 Modal
  const handleAddMember = () => {
    setEditingMember(null)
    form.resetFields()
    setModalVisible(true)
  }

  // 打开编辑成员 Modal
  const handleEditMember = (record) => {
    setEditingMember(record)
    form.setFieldsValue({
      name: record.name,
      email: record.email,
      role: record.role,
    })
    setModalVisible(true)
  }

  // 删除成员
  const handleDeleteMember = (record) => {
    // TODO: 实现删除逻辑
    message.success(`已删除成员：${record.name}`)
  }

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      
      // TODO: 调用 API 保存
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      if (editingMember) {
        message.success('成员信息更新成功')
      } else {
        message.success('成员添加成功')
      }
      
      setModalVisible(false)
      form.resetFields()
    } catch (error) {
      message.error('表单验证失败')
    } finally {
      setLoading(false)
    }
  }

  // 取消操作
  const handleCancel = () => {
    setModalVisible(false)
    form.resetFields()
  }

  // 表格列定义
  const columns = [
    {
      title: '成员',
      dataIndex: 'name',
      key: 'name',
      render: (name, record) => (
        <Space>
          <Avatar src={record.avatar} size={32} />
          <span>{name}</span>
        </Space>
      ),
    },
    {
      title: '角色',
      dataIndex: 'roleName',
      key: 'roleName',
      render: (roleName, record) => {
        const colorMap = {
          owner: 'red',
          admin: 'blue',
          developer: 'green',
        }
        return (
          <Tag color={colorMap[record.role]}>
            {roleName}
          </Tag>
        )
      },
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '部门',
      dataIndex: 'department',
      key: 'department',
    },
    {
      title: '添加时间',
      dataIndex: 'addedAt',
      key: 'addedAt',
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEditMember(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDeleteMember(record)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div className={styles.members}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>成员管理</h1>
          <p className={styles.subtitle}>管理应用成员和角色权限</p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={handleAddMember}
        >
          添加成员
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={memberList}
        rowKey="id"
        pagination={false}
        loading={loading}
      />

      {/* 添加/编辑成员 Modal */}
      <Modal
        title={editingMember ? '编辑成员' : '添加成员'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={handleCancel}
        confirmLoading={loading}
        width={500}
      >
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            role: 'developer',
          }}
        >
          <Form.Item
            label="姓名"
            name="name"
            rules={[{ required: true, message: '请输入姓名' }]}
          >
            <Input placeholder="请输入姓名" />
          </Form.Item>
          <Form.Item
            label="邮箱"
            name="email"
            rules={[
              { required: true, message: '请输入邮箱' },
              { type: 'email', message: '请输入有效的邮箱地址' },
            ]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>
          <Form.Item
            label="角色"
            name="role"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select placeholder="请选择角色">
              {roleOptions.map((option) => (
                <Option key={option.value} value={option.value}>
                  {option.label}
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Members
