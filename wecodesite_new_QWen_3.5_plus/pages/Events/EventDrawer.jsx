import { useState } from 'react'
import { Drawer, Button, Form, Input, Select, Switch, message, Space } from 'antd'
import { eventTypes } from './mock'
import styles from './EventDrawer.module.less'

const { Option } = Select
const { TextArea } = Input

function EventDrawer({ open, onClose, onSubmit, editingEvent }) {
  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm()

  // 设置编辑数据
  useState(() => {
    if (editingEvent) {
      form.setFieldsValue({
        name: editingEvent.name,
        type: editingEvent.type,
        description: editingEvent.description,
        subscribeUrl: editingEvent.subscribeUrl,
        enabled: editingEvent.status === 'enabled',
      })
    } else {
      form.resetFields()
    }
  }, [editingEvent, open])

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      
      // TODO: 调用 API 保存
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      message.success(editingEvent ? '事件配置更新成功' : '事件添加成功')
      onSubmit?.(values)
      form.resetFields()
      onClose()
    } catch (error) {
      message.error('表单验证失败')
    } finally {
      setLoading(false)
    }
  }

  // 取消操作
  const handleCancel = () => {
    form.resetFields()
    onClose()
  }

  return (
    <Drawer
      title={editingEvent ? '编辑事件配置' : '添加事件'}
      placement="right"
      width={600}
      open={open}
      onClose={handleCancel}
      footer={
        <Space>
          <Button onClick={handleCancel}>取消</Button>
          <Button type="primary" loading={loading} onClick={handleSubmit}>
            保存
          </Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical">
        <Form.Item
          label="事件名称"
          name="name"
          rules={[{ required: true, message: '请输入事件名称' }]}
        >
          <Input placeholder="请输入事件名称" />
        </Form.Item>

        <Form.Item
          label="事件类型"
          name="type"
          rules={[{ required: true, message: '请选择事件类型' }]}
        >
          <Select placeholder="请选择事件类型">
            {eventTypes.map((type) => (
              <Option key={type.value} value={type.value}>
                {type.label}
              </Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item
          label="事件描述"
          name="description"
        >
          <TextArea rows={3} placeholder="请输入事件描述" />
        </Form.Item>

        <Form.Item
          label="订阅地址"
          name="subscribeUrl"
          rules={[
            { required: true, message: '请输入订阅地址' },
            { type: 'url', message: '请输入有效的 URL 地址' },
          ]}
        >
          <Input placeholder="https://your-domain.com/events/xxx" />
        </Form.Item>

        <Form.Item
          label="启用状态"
          name="enabled"
          valuePropName="checked"
        >
          <Switch checkedChildren="已启用" unCheckedChildren="未启用" />
        </Form.Item>
      </Form>
    </Drawer>
  )
}

export default EventDrawer
