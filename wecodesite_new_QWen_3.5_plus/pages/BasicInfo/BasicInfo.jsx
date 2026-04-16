import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Form, Input, Button, Radio, Upload, message, Space, Image } from 'antd'
import { EditOutlined, CopyOutlined, EyeOutlined, EyeInvisibleOutlined, UploadOutlined } from '@ant-design/icons'
import { basicInfoData } from './mock'
import styles from './BasicInfo.module.less'

const { TextArea } = Input

function BasicInfo() {
  const [searchParams] = useSearchParams()
  const appId = searchParams.get('appId')

  const [loading, setLoading] = useState(false)
  const [formData, setFormData] = useState(null)
  const [showSecret, setShowSecret] = useState(false)
  
  // 各区块的编辑状态
  const [editingBasic, setEditingBasic] = useState(false)
  const [editingAuth, setEditingAuth] = useState(false)

  const [formBasic] = Form.useForm()
  const [formAuth] = Form.useForm()

  // 加载数据
  useEffect(() => {
    // TODO: 根据 appId 加载真实数据
    setFormData(basicInfoData)
    formBasic.setFieldsValue({
      appName: basicInfoData.appName,
      appNameEn: basicInfoData.appNameEn,
      appDescription: basicInfoData.appDescription,
      appDescriptionEn: basicInfoData.appDescriptionEn,
    })
    formAuth.setFieldsValue({
      authMethod: basicInfoData.authMethod,
    })
  }, [appId])

  // 复制 APP Secret
  const handleCopySecret = () => {
    navigator.clipboard.writeText(basicInfoData.appSecret)
    message.success('APP Secret 已复制到剪贴板')
  }

  // 切换密码显示
  const toggleSecretVisibility = () => {
    setShowSecret(!showSecret)
  }

  // 保存基础信息
  const handleSaveBasic = async () => {
    try {
      const values = await formBasic.validateFields()
      setLoading(true)
      // TODO: 调用 API 保存
      await new Promise(resolve => setTimeout(resolve, 1000)) // 模拟 API 调用
      message.success('基础信息保存成功')
      setEditingBasic(false)
      setFormData(prev => ({ ...prev, ...values }))
    } catch (error) {
      message.error('表单验证失败')
    } finally {
      setLoading(false)
    }
  }

  // 保存认证方式
  const handleSaveAuth = async () => {
    try {
      const values = await formAuth.validateFields()
      setLoading(true)
      // TODO: 调用 API 保存
      await new Promise(resolve => setTimeout(resolve, 1000))
      message.success('认证方式保存成功')
      setEditingAuth(false)
      setFormData(prev => ({ ...prev, ...values }))
    } catch (error) {
      message.error('表单验证失败')
    } finally {
      setLoading(false)
    }
  }

  // 取消编辑
  const handleCancelBasic = () => {
    formBasic.resetFields()
    setEditingBasic(false)
  }

  const handleCancelAuth = () => {
    formAuth.resetFields()
    setEditingAuth(false)
  }

  if (!formData) {
    return <div className={styles.loading}>加载中...</div>
  }

  return (
    <div className={styles.basicInfo}>
      <div className={styles.header}>
        <h1 className={styles.title}>凭证和基础信息</h1>
        <p className={styles.subtitle}>管理应用凭证、安全配置和回调地址</p>
      </div>

      {/* 应用凭证 - 只读 */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>应用凭证</h2>
        </div>
        <div className={styles.content}>
          <div className={styles.formItem}>
            <label className={styles.label}>APP ID</label>
            <div className={styles.value}>{basicInfoData.appId}</div>
          </div>
          <div className={styles.formItem}>
            <label className={styles.label}>APP Secret</label>
            <div className={styles.secretValue}>
              <span className={styles.secretText}>
                {showSecret ? basicInfoData.appSecret : '******'}
              </span>
              <Space>
                <Button
                  type="link"
                  icon={showSecret ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                  onClick={toggleSecretVisibility}
                />
                <Button
                  type="link"
                  icon={<CopyOutlined />}
                  onClick={handleCopySecret}
                />
              </Space>
            </div>
          </div>
        </div>
      </div>

      {/* 基础信息 - 可编辑 */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>基础信息</h2>
          {!editingBasic && (
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => setEditingBasic(true)}
            >
              编辑
            </Button>
          )}
        </div>
        <div className={styles.content}>
          {editingBasic ? (
            <Form form={formBasic} layout="vertical">
              <Form.Item
                label="应用图标"
                name="icon"
              >
                <div className={styles.iconUpload}>
                  <Image
                    src={formData.icon}
                    width={64}
                    height={64}
                    preview={false}
                  />
                  <Button icon={<UploadOutlined />}>上传</Button>
                </div>
              </Form.Item>
              <Form.Item
                label="中文名称"
                name="appName"
                rules={[{ required: true, message: '请输入中文名称' }]}
              >
                <Input placeholder="请输入中文名称" />
              </Form.Item>
              <Form.Item
                label="英文名称"
                name="appNameEn"
                rules={[{ required: true, message: '请输入英文名称' }]}
              >
                <Input placeholder="请输入英文名称" />
              </Form.Item>
              <Form.Item
                label="中文描述"
                name="appDescription"
              >
                <TextArea rows={3} placeholder="请输入中文描述" />
              </Form.Item>
              <Form.Item
                label="英文描述"
                name="appDescriptionEn"
              >
                <TextArea rows={3} placeholder="请输入英文描述" />
              </Form.Item>
              <Form.Item
                label="功能示意图"
                name="functionImage"
              >
                <div className={styles.imagePlaceholder}>
                  <Image
                    src={formData.functionImage || null}
                    fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
                    width={200}
                    height={120}
                    preview={false}
                  />
                </div>
              </Form.Item>
              <Form.Item>
                <Space>
                  <Button
                    type="primary"
                    loading={loading}
                    onClick={handleSaveBasic}
                  >
                    保存
                  </Button>
                  <Button onClick={handleCancelBasic}>取消</Button>
                </Space>
              </Form.Item>
            </Form>
          ) : (
            <div className={styles.infoList}>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>应用图标：</span>
                <Image
                  src={formData.icon}
                  width={64}
                  height={64}
                  preview={true}
                />
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>中文名称：</span>
                <span className={styles.infoValue}>{formData.appName}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>英文名称：</span>
                <span className={styles.infoValue}>{formData.appNameEn}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>中文描述：</span>
                <span className={styles.infoValue}>{formData.appDescription}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>英文描述：</span>
                <span className={styles.infoValue}>{formData.appDescriptionEn}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.infoLabel}>功能示意图：</span>
                {formData.functionImage ? (
                  <Image
                    src={formData.functionImage}
                    width={200}
                    preview={true}
                  />
                ) : (
                  <span className={styles.placeholder}>--</span>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* 认证方式 - 可编辑 */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <h2 className={styles.sectionTitle}>认证方式</h2>
          {!editingAuth && (
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => setEditingAuth(true)}
            >
              编辑
            </Button>
          )}
        </div>
        <div className={styles.content}>
          {editingAuth ? (
            <Form form={formAuth} layout="vertical">
              <Form.Item
                name="authMethod"
                rules={[{ required: true, message: '请选择认证方式' }]}
              >
                <Radio.Group>
                  <Space direction="vertical">
                    <Radio value="cookie">Cookie</Radio>
                    <Radio value="digital_signature">数字签名</Radio>
                    <Radio value="soaheader">SOAHeader</Radio>
                    <Radio value="soaurl">SOAURL</Radio>
                  </Space>
                </Radio.Group>
              </Form.Item>
              <Form.Item>
                <Space>
                  <Button
                    type="primary"
                    loading={loading}
                    onClick={handleSaveAuth}
                  >
                    保存
                  </Button>
                  <Button onClick={handleCancelAuth}>取消</Button>
                </Space>
              </Form.Item>
            </Form>
          ) : (
            <div className={styles.authMethods}>
              <Radio.Group
                value={formData.authMethod}
                disabled
              >
                <Space direction="vertical">
                  <Radio value="cookie">Cookie</Radio>
                  <Radio value="digital_signature">数字签名</Radio>
                  <Radio value="soaheader">SOAHeader</Radio>
                  <Radio value="soaurl">SOAURL</Radio>
                </Space>
              </Radio.Group>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default BasicInfo
