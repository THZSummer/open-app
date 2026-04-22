import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Button,
  Space,
  Card,
} from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { createApi, updateApi } from './thunk';

const { Option } = Select;

function ApiRegister({ visible, api, onSuccess, onCancel }) {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (visible && api) {
      form.setFieldsValue({
        nameCn: api.nameCn,
        nameEn: api.nameEn,
        path: api.path,
        method: api.method,
        categoryId: api.categoryId,
        permissionNameCn: api.permission?.nameCn,
        permissionNameEn: api.permission?.nameEn,
        scope: api.permission?.scope,
      });
    } else {
      form.resetFields();
    }
  }, [visible, api, form]);

  const handleSubmit = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();

      const data = {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        path: values.path,
        method: values.method,
        categoryId: values.categoryId,
        permission: {
          nameCn: values.permissionNameCn,
          nameEn: values.permissionNameEn,
          scope: values.scope,
        },
      };

      let result;
      if (api?.id) {
        result = await updateApi(api.id, data);
      } else {
        result = await createApi(data);
      }

      if (result.code === '200') {
        onSuccess();
      }
    } catch (error) {
      console.error('表单验证失败:', error);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title={api?.id ? '编辑API' : '注册API'}
      open={visible}
      onOk={handleSubmit}
      onCancel={onCancel}
      width={800}
      confirmLoading={submitting}
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Card title="基本信息" size="small" style={{ marginBottom: 16 }}>
          <Form.Item
            label="API名称（中文）"
            name="nameCn"
            rules={[{ required: true, message: '请输入API中文名称' }]}
          >
            <Input placeholder="请输入API中文名称" />
          </Form.Item>

          <Form.Item
            label="API名称（英文）"
            name="nameEn"
            rules={[{ required: true, message: '请输入API英文名称' }]}
          >
            <Input placeholder="请输入API英文名称" />
          </Form.Item>

          <Form.Item
            label="所属分类"
            name="categoryId"
            rules={[{ required: true, message: '请选择所属分类' }]}
          >
            <Select placeholder="请选择所属分类">
              <Option value="1-1-1">发送消息</Option>
              <Option value="1-1-2">用户信息</Option>
            </Select>
          </Form.Item>

          <Form.Item
            label="API路径"
            name="path"
            rules={[
              { required: true, message: '请输入API路径' },
              { pattern: /^\//, message: '路径必须以/开头' },
            ]}
          >
            <Input placeholder="例如：/api/v1/messages" />
          </Form.Item>

          <Form.Item
            label="HTTP方法"
            name="method"
            rules={[{ required: true, message: '请选择HTTP方法' }]}
          >
            <Select placeholder="请选择HTTP方法">
              <Option value="GET">GET</Option>
              <Option value="POST">POST</Option>
              <Option value="PUT">PUT</Option>
              <Option value="DELETE">DELETE</Option>
              <Option value="PATCH">PATCH</Option>
            </Select>
          </Form.Item>
        </Card>

        <Card title="权限信息" size="small">
          <Form.Item
            label="权限名称（中文）"
            name="permissionNameCn"
            rules={[{ required: true, message: '请输入权限中文名称' }]}
          >
            <Input placeholder="请输入权限中文名称" />
          </Form.Item>

          <Form.Item
            label="权限名称（英文）"
            name="permissionNameEn"
            rules={[{ required: true, message: '请输入权限英文名称' }]}
          >
            <Input placeholder="请输入权限英文名称" />
          </Form.Item>

          <Form.Item
            label="Scope标识"
            name="scope"
            rules={[{ required: true, message: '请输入Scope标识' }]}
            extra="格式：api:{模块}:{资源标识}"
          >
            <Input placeholder="api:im:send-message" disabled={!!api?.id} />
          </Form.Item>
        </Card>
      </Form>
    </Modal>
  );
}

export default ApiRegister;