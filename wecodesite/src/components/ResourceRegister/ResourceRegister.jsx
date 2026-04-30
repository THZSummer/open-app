import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  TreeSelect,
  Card,
  Select,
  Radio,
  message,
  Button,
} from 'antd';
import { fetchCategoryTree } from '../../pages/Admin/Category/thunk';
import { convertToTreeData } from '../../utils/common';
import ApprovalNodesConfig from '../ApprovalNodesConfig';
import PropertiesConfig from '../PropertiesConfig';
import { RESOURCE_TYPES, COMMON_BASE_FIELDS } from './constants';

const { Option } = Select;

function ResourceRegister({
  visible,
  resource,
  resourceType = 'callback',
  thunk = {},
  propertyPresets = [],
  transformProperties = null,
  mode = 'create',
  onSuccess,
  onCancel,
}) {
  const { fetchDetail, create, update } = thunk;
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState([]);

  const config = RESOURCE_TYPES[resourceType];

  useEffect(() => {
    if (visible) {
      loadCategories();
    }
  }, [visible]);

  const loadCategories = async () => {
    const result = await fetchCategoryTree();
    if (result && result.code === '200') {
      setCategories(result.data || []);
    } else {
      message.error(result?.message || '加载分类失败');
    }
  };

  useEffect(() => {
    const loadDetail = async () => {
      if (visible && resource?.id && fetchDetail) {
        setLoading(true);
        try {
          const result = await fetchDetail(resource.id);
          if (result && result.code === '200') {
            const data = result.data;
            const properties = transformProperties
              ? transformProperties(data.properties || [], propertyPresets)
              : data.properties || [];
            form.setFieldsValue({
              nameCn: data.nameCn,
              nameEn: data.nameEn,
              categoryId: data.categoryId,
              topic: data.topic,
              permissionNameCn: data.permission?.nameCn,
              permissionNameEn: data.permission?.nameEn,
              scope: data.permission?.scope,
              needApproval: data.permission?.needApproval ?? 1,
              resourceNodes: data.permission?.resourceNodes
                ? JSON.parse(data.permission.resourceNodes)
                : [],
              properties: properties,
            });
          } else {
            message.error(result?.message || '加载详情失败');
          }
        } finally {
          setLoading(false);
        }
      } else if (visible) {
        form.resetFields();
      }
    };

    loadDetail();
  }, [visible, resource, form, fetchDetail]);

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
        topic: values.topic,
        permission: {
          nameCn: values.permissionNameCn,
          nameEn: values.permissionNameEn,
          scope: values.scope,
          needApproval: values.needApproval ?? 1,
          resourceNodes: values.resourceNodes && values.resourceNodes.length > 0
            ? JSON.stringify(values.resourceNodes)
            : null,
        },
        properties: properties,
      };

      let result;
      const apiMethod = resource?.id ? update : create;
      const successMessage = resource?.id ? '更新成功' : '注册成功';
      const errorMessage = resource?.id ? '更新失败' : '注册失败';

      if (apiMethod) {
        result = await apiMethod(resource?.id, data);
        if (result && result.code === '200') {
          message.success(successMessage);
          onSuccess?.();
        } else {
          message.error(result?.message || errorMessage);
        }
      }
    } catch (error) {
      console.error('表单验证失败:', error);
    } finally {
      setSubmitting(false);
    }
  };

  const renderField = (fieldConfig) => {
    const label = fieldConfig.label || config.labels[fieldConfig.labelKey];
    const placeholder = fieldConfig.placeholder || (fieldConfig.placeholderPrefix ? `${fieldConfig.placeholderPrefix}${label}` : '');
    const processedRules = (fieldConfig.rules || []).map(rule => ({
      ...rule,
      message: rule.message || `请输入${label}`,
    }));

    const commonProps = {
      label,
      name: fieldConfig.name,
      rules: processedRules,
      extra: fieldConfig.extra,
      initialValue: fieldConfig.initialValue,
    };

    switch (fieldConfig.type) {
      case 'Input':
        return (
          <Form.Item key={fieldConfig.key} {...commonProps}>
            <Input placeholder={placeholder} disabled={mode === 'view'} />
          </Form.Item>
        );
      case 'TreeSelect':
        return (
          <Form.Item key={fieldConfig.key} {...commonProps}>
            <TreeSelect
              placeholder={placeholder}
              treeData={convertToTreeData(categories)}
              treeDefaultExpandAll
              style={{ width: '100%' }}
              dropdownStyle={{ maxHeight: 400, overflow: 'auto' }}
              disabled={mode === 'view'}
            />
          </Form.Item>
        );
      case 'Select':
        return (
          <Form.Item key={fieldConfig.key} {...commonProps}>
            <Select placeholder={placeholder} disabled={mode === 'view'}>
              {(fieldConfig.options || []).map(opt => (
                <Option key={opt.value} value={opt.value}>{opt.label}</Option>
              ))}
            </Select>
          </Form.Item>
        );
      default:
        return null;
    }
  };

  const renderBaseInfoFields = () => {
    const allFields = [...COMMON_BASE_FIELDS, ...(config.baseFields || [])];
    return allFields.map(field => renderField(field));
  };

  const getTitle = () => {
    if (mode === 'view') return `查看${config.title}详情`;
    if (mode === 'edit') return `编辑${config.title}`;
    return `注册${config.title}`;
  };

  return (
    <Modal
      title={getTitle()}
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
          {renderBaseInfoFields()}
        </Card>

        <Card title="权限信息" size="small" style={{ marginBottom: 16 }}>
          <Form.Item
            label={config.labels.permissionNameCn}
            name="permissionNameCn"
            rules={[{ required: true, message: `请输入${config.labels.permissionNameCn}` }]}
          >
            <Input placeholder={`请输入${config.labels.permissionNameCn}`} disabled={mode === 'view'} />
          </Form.Item>

          <Form.Item
            label={config.labels.permissionNameEn}
            name="permissionNameEn"
            rules={[
              { required: true, message: `请输入${config.labels.permissionNameEn}` },
              { pattern: /^[a-zA-Z0-9\s\-_()]+$/, message: '英文名称不能输入中文字符' }
            ]}
          >
            <Input placeholder={`请输入${config.labels.permissionNameEn}`} disabled={mode === 'view'} />
          </Form.Item>

          <Form.Item
            label="Scope标识"
            name="scope"
            rules={[
              { required: true, message: '请输入Scope标识' },
              { pattern: config.scopePattern, message: config.scopeMessage }
            ]}
            extra={config.scopeExtra}
          >
            <Input placeholder={config.labels.scope} disabled={mode === 'view'} />
          </Form.Item>

          <Form.Item
            label="是否需要审批"
            name="needApproval"
            initialValue={1}
            tooltip="开启后，消费方申请此权限时需要审批"
          >
            <Radio.Group disabled={mode === 'view'}>
              <Radio value={1}>需要审批</Radio>
              <Radio value={0}>无需审批</Radio>
            </Radio.Group>
          </Form.Item>

          <Form.Item noStyle shouldUpdate={(prev, cur) => prev.needApproval !== cur.needApproval}>
            {({ getFieldValue }) => (
              <ApprovalNodesConfig
                form={form}
                mode={mode}
                needApproval={getFieldValue('needApproval')}
              />
            )}
          </Form.Item>
        </Card>

        <PropertiesConfig
          form={form}
          mode={mode}
          propertyPresets={propertyPresets}
          title="扩展属性（可选）"
        />
      </Form>
    </Modal>
  );
}

export default ResourceRegister;
