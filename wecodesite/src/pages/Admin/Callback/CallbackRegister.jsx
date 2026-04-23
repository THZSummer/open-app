import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  TreeSelect,
  Card,
  Space,
  Button,
  Select,
} from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { createCallback, updateCallback, fetchCallbackDetail } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';

// 回调扩展属性预设选项
const CALLBACK_PROPERTY_PRESETS = [
  { value: 'descriptionCn', label: '中文描述', placeholder: '回调的中文描述' },
  { value: 'descriptionEn', label: '英文描述', placeholder: 'Callback description in English' },
  { value: 'docUrl', label: '文档链接', placeholder: 'https://docs.example.com/callback/xxx' },
  { value: 'timeout', label: '超时时间', placeholder: '30000 (毫秒)' },
  { value: 'retryCount', label: '重试次数', placeholder: '3' },
  { value: '__custom__', label: '自定义...', placeholder: '输入自定义属性名' },
];

function CallbackRegister({ visible, callback, onSuccess, onCancel }) {
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);
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
    const loadCallbackDetail = async () => {
      if (visible && callback?.id) {
        setLoading(true);
        try {
          const result = await fetchCallbackDetail(callback.id);
          if (result.code === '200') {
            const data = result.data;
            form.setFieldsValue({
              nameCn: data.nameCn,
              nameEn: data.nameEn,
              categoryId: data.categoryId,
              permissionNameCn: data.permission?.nameCn,
              permissionNameEn: data.permission?.nameEn,
              scope: data.permission?.scope,
              properties: data.properties || [],
            });
          }
        } finally {
          setLoading(false);
        }
      } else if (visible) {
        form.resetFields();
      }
    };

    loadCallbackDetail();
  }, [visible, callback, form]);

  const handleSubmit = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();

      const properties = values.properties?.map(prop => {
        if (prop.propertyName === '__custom__') {
          return {
            propertyName: prop.customPropertyName,
            propertyValue: prop.propertyValue,
          };
        }
        return prop;
      }).filter(prop => prop.propertyName);

      const data = {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        categoryId: values.categoryId,
        permission: {
          nameCn: values.permissionNameCn,
          nameEn: values.permissionNameEn,
          scope: values.scope,
        },
        properties: properties,
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
      loading={loading}
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
            {(fields, { add, remove }) => {
              const formValues = form.getFieldValue('properties') || [];
              const usedPresets = formValues
                .map(item => item?.propertyName)
                .filter(name => name && name !== '__custom__');

              return (
                <>
                  {fields.map(({ key, name, ...restField }) => (
                    <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                      <Form.Item
                        {...restField}
                        name={[name, 'propertyName']}
                        rules={[{ required: true, message: '请选择或输入属性名' }]}
                      >
                        <Select
                          placeholder="选择属性"
                          style={{ width: 160 }}
                          onChange={(value) => {
                            const properties = form.getFieldValue('properties');
                            properties[name].propertyValue = undefined;
                            form.setFieldsValue({ properties });
                          }}
                        >
                          {CALLBACK_PROPERTY_PRESETS.map(preset => (
                            <Select.Option 
                              key={preset.value} 
                              value={preset.value}
                              disabled={preset.value !== '__custom__' && usedPresets.includes(preset.value)}
                            >
                              {preset.label}
                            </Select.Option>
                          ))}
                        </Select>
                      </Form.Item>
                      
                      <Form.Item
                        noStyle
                        shouldUpdate={(prev, cur) => 
                          prev.properties?.[name]?.propertyName !== cur.properties?.[name]?.propertyName
                        }
                      >
                        {({ getFieldValue }) => {
                          const propertyName = getFieldValue(['properties', name, 'propertyName']);
                          if (propertyName === '__custom__') {
                            return (
                              <Form.Item
                                {...restField}
                                name={[name, 'customPropertyName']}
                                rules={[{ required: true, message: '请输入自定义属性名' }]}
                              >
                                <Input placeholder="自定义属性名" style={{ width: 140 }} />
                              </Form.Item>
                            );
                          }
                          return null;
                        }}
                      </Form.Item>

                      <Form.Item
                        noStyle
                        shouldUpdate={(prev, cur) => 
                          prev.properties?.[name]?.propertyName !== cur.properties?.[name]?.propertyName
                        }
                      >
                        {({ getFieldValue }) => {
                          const propertyName = getFieldValue(['properties', name, 'propertyName']);
                          const preset = CALLBACK_PROPERTY_PRESETS.find(p => p.value === propertyName);
                          const isCustom = propertyName === '__custom__';
                          
                          return (
                            <Form.Item
                              {...restField}
                              name={[name, 'propertyValue']}
                              rules={[{ required: true, message: '请输入属性值' }]}
                            >
                              <Input 
                                placeholder={isCustom ? '属性值' : (preset?.placeholder || '属性值')} 
                                style={{ width: 260 }} 
                              />
                            </Form.Item>
                          );
                        }}
                      </Form.Item>

                      <MinusCircleOutlined onClick={() => remove(name)} />
                    </Space>
                  ))}
                  <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                    添加属性
                  </Button>
                </>
              );
            }}
          </Form.List>
        </Card>
      </Form>
    </Modal>
  );
}

export default CallbackRegister;
