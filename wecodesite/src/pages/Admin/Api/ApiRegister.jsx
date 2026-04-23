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
import { createApi, updateApi, fetchApiDetail } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';

const { Option } = Select;

// API 扩展属性预设选项
const API_PROPERTY_PRESETS = [
  { value: 'descriptionCn', label: '中文描述', placeholder: 'API的中文描述' },
  { value: 'descriptionEn', label: '英文描述', placeholder: 'API description in English' },
  { value: 'docUrl', label: '文档链接', placeholder: 'https://docs.example.com/api/xxx' },
  { value: 'rateLimit', label: '速率限制', placeholder: '100/minute' },
  { value: '__custom__', label: '自定义...', placeholder: '输入自定义属性名' },
];

// 认证方式选项
const AUTH_TYPE_OPTIONS = [
  { value: 0, label: 'Cookie' },
  { value: 1, label: 'SOA' },
  { value: 2, label: 'APIG' },
  { value: 3, label: 'IAM' },
  { value: 4, label: '免认证' },
  { value: 5, label: 'AKSK' },
  { value: 6, label: 'CLITOKEN' },
];

function ApiRegister({ visible, api, mode = 'create', onSuccess, onCancel }) {
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
    const loadApiDetail = async () => {
      if (visible && api?.id) {
        setLoading(true);
        try {
          const result = await fetchApiDetail(api.id);
          if (result.code === '200') {
            const data = result.data;
            form.setFieldsValue({
              nameCn: data.nameCn,
              nameEn: data.nameEn,
              path: data.path,
              method: data.method,
              authType: data.authType,
              categoryId: data.categoryId,
              permissionNameCn: data.permission?.nameCn,
              permissionNameEn: data.permission?.nameEn,
              scope: data.permission?.scope,
              properties: data.properties?.map(prop => ({
                propertyName: API_PROPERTY_PRESETS.find(p => p.value === prop.propertyName)
                  ? prop.propertyName
                  : '__custom__',
                propertyValue: prop.propertyValue,
                customPropertyName: API_PROPERTY_PRESETS.find(p => p.value === prop.propertyName)
                  ? undefined
                  : prop.propertyName,
              })) || [],
            });
          }
        } finally {
          setLoading(false);
        }
      } else if (visible) {
        form.resetFields();
      }
    };

    loadApiDetail();
  }, [visible, api, form]);

  const handleSubmit = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();

      // 处理 properties，将 __custom__ 替换为用户输入的自定义属性名
      const properties = values.properties?.map(prop => {
        if (prop.propertyName === '__custom__') {
          return {
            propertyName: prop.customPropertyName,
            propertyValue: prop.propertyValue,
          };
        }
        return prop;
      }).filter(prop => prop.propertyName); // 过滤掉空的

      const data = {
        nameCn: values.nameCn,
        nameEn: values.nameEn,
        path: values.path,
        method: values.method,
        authType: values.authType,
        categoryId: values.categoryId,
        permission: {
          nameCn: values.permissionNameCn,
          nameEn: values.permissionNameEn,
          scope: values.scope,
        },
        properties: properties,
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
      title={
        mode === 'view' ? '查看API详情' :
        mode === 'edit' ? '编辑API' :
        '注册API'
      }
      open={visible}
      onOk={handleSubmit}
      onCancel={onCancel}
      width={800}
      confirmLoading={submitting}
      loading={loading}
      destroyOnClose
      footer={mode === 'view' ? [
        <Button key="close" onClick={onCancel}>
          关闭
        </Button>
      ] : undefined}
    >
      <Form form={form} layout="vertical">
        <Card title="基本信息" size="small" style={{ marginBottom: 16 }}>
          <Form.Item
            label="API名称（中文）"
            name="nameCn"
            rules={[{ required: true, message: '请输入API中文名称' }]}
          >
            <Input placeholder="请输入API中文名称" disabled={mode === 'view'} />
          </Form.Item>

          <Form.Item
            label="API名称（英文）"
            name="nameEn"
            rules={[{ required: true, message: '请输入API英文名称' }]}
          >
            <Input placeholder="请输入API英文名称" disabled={mode === 'view'} />
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
              disabled={mode === 'view'}
            />
          </Form.Item>

          <Form.Item
            label="API路径"
            name="path"
            rules={[
              { required: true, message: '请输入API路径' },
              { pattern: /^\//, message: '路径必须以/开头' },
            ]}
          >
            <Input placeholder="例如：/api/v1/messages" disabled={mode === 'view'} />
          </Form.Item>

          <Form.Item
            label="HTTP方法"
            name="method"
            rules={[{ required: true, message: '请选择HTTP方法' }]}
          >
            <Select placeholder="请选择HTTP方法" disabled={mode === 'view'}>
              <Option value="GET">GET</Option>
              <Option value="POST">POST</Option>
              <Option value="PUT">PUT</Option>
              <Option value="DELETE">DELETE</Option>
              <Option value="PATCH">PATCH</Option>
            </Select>
          </Form.Item>

          <Form.Item
            label="认证方式"
            name="authType"
            initialValue={1}
            extra="默认为 SOA 认证"
          >
            <Select placeholder="请选择认证方式" disabled={mode === 'view'}>
              {AUTH_TYPE_OPTIONS.map(opt => (
                <Option key={opt.value} value={opt.value}>{opt.label}</Option>
              ))}
            </Select>
          </Form.Item>
        </Card>

        <Card title="权限信息" size="small">
          <Form.Item
            label="权限名称（中文）"
            name="permissionNameCn"
            rules={[{ required: true, message: '请输入权限中文名称' }]}
          >
            <Input placeholder="请输入权限中文名称" disabled={mode === 'view'} />
          </Form.Item>

          <Form.Item
            label="权限名称（英文）"
            name="permissionNameEn"
            rules={[{ required: true, message: '请输入权限英文名称' }]}
          >
            <Input placeholder="请输入权限英文名称" disabled={mode === 'view'} />
          </Form.Item>

          <Form.Item
            label="Scope标识"
            name="scope"
            rules={[{ required: true, message: '请输入Scope标识' }]}
            extra="格式：api:{模块}:{资源标识}"
          >
            <Input placeholder="api:im:send-message" disabled={mode === 'view'} />
          </Form.Item>
        </Card>

        <Card title="扩展属性（可选）" size="small">
          <Form.List name="properties">
            {(fields, { add, remove }) => {
              // 获取当前已选择的预设属性
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
                           disabled={mode === 'view'}
                           onChange={(value) => {
                             // 切换属性时清空属性值
                             const properties = form.getFieldValue('properties');
                             properties[name].propertyValue = undefined;
                             form.setFieldsValue({ properties });
                           }}
                         >
                          {API_PROPERTY_PRESETS.map(preset => (
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
                      
                      {/* 当选择"自定义"时显示自定义属性名输入框 */}
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
                                <Input placeholder="自定义属性名" style={{ width: 140 }} disabled={mode === 'view'} />
                              </Form.Item>
                            );
                          }
                          return null;
                        }}
                      </Form.Item>

                      {/* 属性值输入框 */}
                      <Form.Item
                        noStyle
                        shouldUpdate={(prev, cur) => 
                          prev.properties?.[name]?.propertyName !== cur.properties?.[name]?.propertyName
                        }
                      >
                        {({ getFieldValue }) => {
                          const propertyName = getFieldValue(['properties', name, 'propertyName']);
                          const preset = API_PROPERTY_PRESETS.find(p => p.value === propertyName);
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
                                 disabled={mode === 'view'}
                               />
                             </Form.Item>
                          );
                        }}
                      </Form.Item>

                      {mode !== 'view' && <MinusCircleOutlined onClick={() => remove(name)} />}
                    </Space>
                  ))}
                  {mode !== 'view' && (
                    <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                      添加属性
                    </Button>
                  )}
                </>
              );
            }}
          </Form.List>
        </Card>
      </Form>
    </Modal>
  );
}

export default ApiRegister;