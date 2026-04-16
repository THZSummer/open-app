import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Form, Input, Button, Switch, message, Space, Card } from 'antd'
import { capabilities } from '../Capabilities/mock'

function CapabilityDetail() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const appId = searchParams.get('appId')
  const type = searchParams.get('type')

  const [loading, setLoading] = useState(false)
  const [capability, setCapability] = useState(null)
  const [form] = Form.useForm()

  useEffect(() => {
    if (type) {
      const cap = capabilities.find((c) => c.type === type)
      if (cap) {
        setCapability(cap)
        form.setFieldsValue({
          enabled: cap.status === 'enabled',
        })
      }
    }
  }, [type])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      
      // TODO: 调用 API 保存
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      message.success('能力配置保存成功')
    } catch (error) {
      message.error('表单验证失败')
    } finally {
      setLoading(false)
    }
  }

  const handleBack = () => {
    const params = new URLSearchParams()
    if (appId) params.set('appId', appId)
    navigate(`/capabilities?${params.toString()}`)
  }

  if (!capability) {
    return <div>加载中...</div>
  }

  return (
    <div style={{ maxWidth: 800 }}>
      <div style={{ marginBottom: 24 }}>
        <Button onClick={handleBack}>返回</Button>
      </div>

      <Card
        title={
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span style={{ fontSize: 32 }}>{capability.icon}</span>
            <div>
              <h2 style={{ margin: 0 }}>{capability.name}</h2>
              <p style={{ margin: 0, color: 'var(--text-secondary)' }}>
                {capability.description}
              </p>
            </div>
          </div>
        }
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="启用状态"
            name="enabled"
            valuePropName="checked"
          >
            <Switch checkedChildren="已启用" unCheckedChildren="未启用" />
          </Form.Item>

          {type === 'bot' && (
            <>
              <Form.Item
                label="机器人名称"
                name="botName"
                rules={[{ required: true, message: '请输入机器人名称' }]}
              >
                <Input placeholder="请输入机器人名称" />
              </Form.Item>
              <Form.Item
                label="回调地址"
                name="callbackUrl"
                rules={[
                  { required: true, message: '请输入回调地址' },
                  { type: 'url', message: '请输入有效的 URL 地址' },
                ]}
              >
                <Input placeholder="https://your-domain.com/callback" />
              </Form.Item>
            </>
          )}

          {type === 'message' && (
            <>
              <Form.Item
                label="消息模板"
                name="messageTemplate"
              >
                <Input.TextArea
                  rows={4}
                  placeholder="请输入消息模板内容"
                />
              </Form.Item>
            </>
          )}

          {type === 'calendar' && (
            <>
              <Form.Item
                label="默认提醒时间"
                name="defaultReminder"
              >
                <Input placeholder="例如：提前 15 分钟" />
              </Form.Item>
            </>
          )}

          <Form.Item>
            <Space>
              <Button
                type="primary"
                loading={loading}
                onClick={handleSubmit}
              >
                保存配置
              </Button>
              <Button onClick={handleBack}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default CapabilityDetail
