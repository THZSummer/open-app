import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  TreeSelect,
  Card,
  Space,
  Button,
} from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { createCallback, updateCallback } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';

function CallbackRegister({ visible, callback, onSuccess, onCancel }) {
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
    if (visible && callback) {
      // 编辑模式：填充表单
      form.setFieldsValue({
        nameCn: callback.nameCn,
        nameEn: callback.nameEn,
        categoryId: callback.categoryId,
        permissionNameCn: callback.permission?.nameCn,
        permissionNameEn: callback.permission?.nameEn,
        scope: callback.permission?.scope,
        properties: callback.properties || [],
      });
    } else {
      // 新增模式：重置表单
      form.resetFields();
    }
  }, [visible, callback, form]);

  const handleSubmit = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();

      const data = {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        categoryId: values.categoryId,
        permission: {
          nameCn: values.permissionNameCn,
          nameEn: values.permissionNameEn,
          scope: values.scope,
        },
        properties: values.properties,
      };

      let result;
      if (callback?.id) {
        result = await updateCallback(callback.id, data);
      } else {
        result = await createCallback(data);
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
      title={callback?.id ? '编辑回调' : '注册回调'}
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
            rules={[{ required: true, message: '请输入回调中文名称' }]}
          >
            <Input placeholder="请输入回调中文名称" />
          </Form.Item>

          <Form.Item
            label="英文名称"
            name="nameEn"
            rules={[{ required: true, message: '请输入回调英文名称' }]}
          >
            <Input placeholder="请输入回调英文名称" />
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
              allowClear
              style={{ width: '100%' }}
              dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
            />
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
            extra="格式：callback:{模块}:{资源标识}"
          >
            <Input placeholder="callback:approval:completed" disabled={!!callback?.id} />
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
                      name={[name, 'key']}
                      rules={[{ required: true, message: '请输入属性名' }]}
                    >
                      <Input placeholder="属性名" style={{ width: 200 }} />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'value']}
                      rules={[{ required: true, message: '请输入属性值' }]}
                    >
                      <Input placeholder="属性值" style={{ width: 300 }} />
                    </Form.Item>
                    <Form.Item
                      {...restField}
                      name={[name, 'description']}
                    >
                      <Input placeholder="描述（可选）" style={{ width: 200 }} />
                    </Form.Item>
                    <MinusCircleOutlined onClick={() => remove(name)} />
                  </Space>
                ))}
                <Form.Item>
                  <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                    添加扩展属性
                  </Button>
                </Form.Item>
              </>
            )}
          </Form.List>
        </Card>
      </Form>
    </Modal>
  );
}

export default CallbackRegister;
