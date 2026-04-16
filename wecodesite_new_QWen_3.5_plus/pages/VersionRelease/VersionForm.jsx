import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Form, Input, Button, message, Space, Card } from 'antd'
import { versionList } from './mock'
import styles from './VersionForm.module.less'

const { TextArea } = Input

function VersionForm() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const appId = searchParams.get('appId')
  const versionId = searchParams.get('versionId')

  const [loading, setLoading] = useState(false)
  const [form] = Form.useForm()
  const [isEdit, setIsEdit] = useState(false)
  const [versionData, setVersionData] = useState(null)

  useEffect(() => {
    if (versionId) {
      // 编辑模式
      const version = versionList.find(v => v.id === versionId)
      if (version) {
        setVersionData(version)
        setIsEdit(true)
        form.setFieldsValue({
          version: version.version,
          name: version.name,
          description: version.description,
          changelog: version.changelog,
        })
      }
    }
  }, [versionId])

  // 提交表单
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      
      // TODO: 调用 API 保存
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      message.success(isEdit ? '版本信息更新成功' : '版本创建成功')
      
      // 返回版本列表
      const params = new URLSearchParams()
      if (appId) params.set('appId', appId)
      navigate(`/version-release?${params.toString()}`)
    } catch (error) {
      message.error('表单验证失败')
    } finally {
      setLoading(false)
    }
  }

  // 取消操作
  const handleCancel = () => {
    const params = new URLSearchParams()
    if (appId) params.set('appId', appId)
    navigate(`/version-release?${params.toString()}`)
  }

  return (
    <div className={styles.versionForm}>
      <Card
        title={isEdit ? `编辑版本：${versionData?.version}` : '发布新版本'}
        className={styles.card}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="版本号"
            name="version"
            rules={[
              { required: true, message: '请输入版本号' },
              { pattern: /^v?\d+\.\d+\.\d+$/, message: '请输入正确的版本号格式（如 v1.0.0）' },
            ]}
          >
            <Input placeholder="请输入版本号，如 v1.0.0" />
          </Form.Item>

          <Form.Item
            label="版本名称"
            name="name"
            rules={[{ required: true, message: '请输入版本名称' }]}
          >
            <Input placeholder="请输入版本名称" />
          </Form.Item>

          <Form.Item
            label="版本描述"
            name="description"
            rules={[{ required: true, message: '请输入版本描述' }]}
          >
            <TextArea rows={3} placeholder="请输入版本描述" />
          </Form.Item>

          <Form.Item
            label="更新日志"
            name="changelog"
            rules={[{ required: true, message: '请输入更新日志' }]}
          >
            <TextArea
              rows={6}
              placeholder="请输入更新日志，每行一条更新内容"
            />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button
                type="primary"
                loading={loading}
                onClick={handleSubmit}
              >
                {isEdit ? '保存' : '发布版本'}
              </Button>
              <Button onClick={handleCancel}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default VersionForm
