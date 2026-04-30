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
import { fetchCategoryTree } from '../../pages/admin/Category/thunk';
import { convertToTreeData } from '../../utils/common';
import ApprovalNodesConfig from '../ApprovalNodesConfig';
import PropertiesConfig from '../PropertiesConfig';
import { RESOURCE_TYPES } from './constants';

const { Option } = Select;

function ResourceRegister({
  visible,
  resource,
  resourceType = 'callback',
  thunk = {},
  propertyPresets = [],
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
              properties: data.properties || [],
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

  const renderBaseInfoFields = () => {
    const fields = [];

    fields.push(
      <Form.Item
        key="nameCn"
        label={config.labels.nameCn}
        name="nameCn"
        rules={[{ required: true, message: `请输入${config.labels.nameCn}` }]}
      >
        <Input placeholder={`请输入${config.labels.nameCn}`} disabled={mode === 'view'} />
      </Form.Item>
    );

    fields.push(
      <Form.Item
        key="nameEn"
        label={config.labels.nameEn}
        name="nameEn"
        rules={[
          { required: true, message: `请输入${config.labels.nameEn}` },
          { pattern: /^[a-zA-Z0-9\s\-_()]+$/, message: '英文名称不能输入中文字符' }
        ]}
      >
        <Input placeholder={`请输入${config.labels.nameEn}`} disabled={mode === 'view'} />
      </Form.Item>
    );

    fields.push(
      <Form.Item
        key="categoryId"
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
    );

    if (config.baseInfoFields.includes('topic')) {
      fields.push(
        <Form.Item
          key="topic"
          label="Topic 标识"
          name="topic"
          rules={[
            { required: true, message: '请输入 Topic 标识' },
            { pattern: config.topicPattern, message: config.topicMessage },
          ]}
          extra={config.topicExtra}
        >
          <Input placeholder={config.labels.topic} disabled={mode === 'view'} />
        </Form.Item>
      );
    }

    if (config.baseInfoFields.includes('path')) {
      fields.push(
        <Form.Item
          key="path"
          label="API路径"
          name="path"
          rules={[
            { required: true, message: '请输入API路径' },
            { pattern: /^\//, message: '路径必须以/开头' },
          ]}
        >
          <Input placeholder="例如：/api/v1/messages" disabled={mode === 'view'} />
        </Form.Item>
      );
    }

    if (config.baseInfoFields.includes('method')) {
      fields.push(
        <Form.Item
          key="method"
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
      );
    }

    if (config.baseInfoFields.includes('authType')) {
      fields.push(
        <Form.Item
          key="authType"
          label="认证方式"
          name="authType"
          initialValue={1}
          extra="默认为 SOA 认证"
        >
          <Select placeholder="请选择认证方式" disabled={mode === 'view'}>
            <Option value={1}>SOA认证</Option>
            <Option value={2}>Token认证</Option>
          </Select>
        </Form.Item>
      );
    }

    return fields;
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
