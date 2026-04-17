import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Form, Input, Button, message } from 'antd';
import { ArrowLeftOutlined } from '@ant-design/icons';
import { createVersion } from './thunk';
import styles from './VersionForm.module.less';

const VersionForm: React.FC = () => {
  const navigate = useNavigate();
  const [form] = Form.useForm();

  const handleSubmit = async () => {
    const values = await form.validateFields();
    await createVersion(values);
    message.success('版本创建成功');
    navigate('..');
  };

  const handleGoBack = () => {
    navigate('..');
  };

  return (
    <div className={styles.container}>
      <div className={styles.backLink} onClick={handleGoBack}>
        <ArrowLeftOutlined /> 返回版本发布
      </div>

      <h2 className={styles.title}>创建新版本</h2>

      <Card className={styles.card}>
        <Form form={form} layout="vertical">
          <Form.Item
            label="版本号"
            name="version"
            rules={[{ required: true, message: '请输入版本号' }]}
          >
            <Input placeholder="例如: 1.0.0" />
          </Form.Item>

          <Form.Item
            label="版本描述"
            name="description"
            rules={[{ required: true, message: '请输入版本描述' }]}
          >
            <Input.TextArea rows={4} placeholder="请输入版本描述" />
          </Form.Item>

          <Form.Item>
            <div className={styles.actions}>
              <Button onClick={handleGoBack}>取消</Button>
              <Button type="primary" onClick={handleSubmit} style={{ marginLeft: 8 }}>
                提交审核
              </Button>
            </div>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default VersionForm;
