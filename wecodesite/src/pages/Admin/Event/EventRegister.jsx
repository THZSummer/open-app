import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  TreeSelect,
  Button,
  Space,
  Card,
} from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { createEvent, updateEvent } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';

const { Option } = Select;

function EventRegister({ visible, event, onSuccess, onCancel }) {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    if (visible) {
      loadCategories();
    }
  }, [visible]);

  const loadCategories = async () => {
    const result = await fetchCategoryTree();
    if (result.code === '200') {
      setCategories(result.data || []);
    }
  };

  // 将后端返回的分类树数据转换为 TreeSelect 所需格式
  const convertToTreeData = (categories) => {
    if (!categories) return [];
    return categories.map(cat => ({
      value: cat.id,
      title: cat.nameCn,
      key: cat.id,
      children: cat.children ? convertToTreeData(cat.children) : undefined
    }));
  };

  useEffect(() => {
    if (visible && event) {
      // 编辑模式：填充表单
      form.setFieldsValue({
        nameCn: event.nameCn,
        nameEn: event.nameEn,
        categoryId: event.categoryId,
        topic: event.topic,
        permissionNameCn: event.permission?.nameCn,
        permissionNameEn: event.permission?.nameEn,
        scope: event.permission?.scope,
        properties: event.properties || [],
      });
    } else {
      // 新增模式：重置表单
      form.resetFields();
    }
  }, [visible, event, form]);

  const handleSubmit = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();

      const data = {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        topic: values.topic,
        categoryId: values.categoryId,
        permission: {
          nameCn: values.permissionNameCn,
          nameEn: values.permissionNameEn,
          scope: values.scope,
        },
        properties: values.properties,
      };

      let result;
      if (event?.id) {
        result = await updateEvent(event.id, data);
      } else {
        result = await createEvent(data);
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
      title={event?.id ? '编辑事件' : '注册事件'}
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
            label="中文名称"
            name="nameCn"
            rules={[{ required: true, message: '请输入事件中文名称' }]}
          >
            <Input placeholder="请输入事件中文名称" />
          </Form.Item>

          <Form.Item
            label="英文名称"
            name="nameEn"
            rules={[{ required: true, message: '请输入事件英文名称' }]}
          >
            <Input placeholder="请输入事件英文名称" />
          </Form.Item>

          <Form.Item
            label="所属分类"
            name="categoryId"
            rules={[{ required: true, message: '请选择所属分类' }]}
          >
            <TreeSelect
              placeholder="请选择所属分类"
              treeData={convertToTreeData(categories)}
              treeDefaultExpandAll
              style={{ width: '100%' }}
              dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
            />
          </Form.Item>

          <Form.Item
            label="Topic 标识"
            name="topic"
            rules={[
              { required: true, message: '请输入 Topic 标识' },
              { pattern: /^[a-zA-Z][a-zA-Z0-9]*\.[a-zA-Z][a-zA-Z0-9.]*/, message: '格式不正确，应为：模块.事件' },
            ]}
            extra="格式：模块.事件，例如：user.status, msg.send.ok"
          >
            <Input placeholder="user.status" />
          </Form.Item>
        </Card>

        <Card title="权限信息" size="small" style={{ marginBottom: 16 }}>
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
            extra="格式：event:{模块}:{事件标识}"
          >
            <Input placeholder="event:im:message-received" disabled={!!event?.id} />
          </Form.Item>
        </Card>

        <Card title="扩展属性（可选）" size="small">
          <Form.List name="properties">
            {(fields, { add, remove }) => (
              <>
                {fields.map(({ key, name, ...restField }) => (
                  <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                    <Form.Item
                      {...restField}
                      name={[name, 'propertyName']}
                      rules={[{ required: true, message: '请输入属性名' }]}
                    >
                      <Input placeholder="属性名，如 descriptionCn" style={{ width: 200 }} />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'propertyValue']}
                      rules={[{ required: true, message: '请输入属性值' }]}
                    >
                      <Input placeholder="属性值" style={{ width: 300 }} />
                    </Form.Item>
                    <MinusCircleOutlined onClick={() => remove(name)} />
                  </Space>
                ))}
                <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                  添加属性
                </Button>
              </>
            )}
          </Form.List>
        </Card>
      </Form>
    </Modal>
  );
}

export default EventRegister;
