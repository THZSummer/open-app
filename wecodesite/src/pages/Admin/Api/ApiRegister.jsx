import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  TreeSelect,
  Card,
  Radio,
  Button,
  message,
} from 'antd';
import { createApi, updateApi, fetchApiDetail } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import { API_PROPERTY_PRESETS, AUTH_TYPE_OPTIONS, HTTP_METHOD_OPTIONS } from './constants';
import { convertToTreeData } from '../../../utils/common';
import ApprovalNodesConfig from '../../../components/ApprovalNodesConfig';
import PropertiesConfig from '../../../components/PropertiesConfig';

const { Option } = Select;

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
    if (result && result.code === '200') {
      setCategories(result.data || []);
    } else {
      message.error(result?.message || '加载分类失败');
    }
  };

  useEffect(() => {
    const loadApiDetail = async () => {
      if (visible && api?.id) {
        setLoading(true);
        try {
          const result = await fetchApiDetail(api.id);
          if (result && result.code === '200') {
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
              needApproval: data.permission?.needApproval ?? 1,
              resourceNodes: data.permission?.resourceNodes
                ? JSON.parse(data.permission.resourceNodes)
                : [],
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
          } else {
            message.error(result?.message || '加载API详情失败');
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
          // v2.8.0新增：审批配置字段
          needApproval: values.needApproval ?? 1,
          // 将 resourceNodes 数组转换为 JSON 字符串
          resourceNodes: values.resourceNodes && values.resourceNodes.length > 0
            ? JSON.stringify(values.resourceNodes)
            : null,
        },
        properties: properties,
      };

      let result;
      if (api?.id) {
        result = await updateApi(api.id, data);
        if (result && result.code === '200') {
          message.success('更新成功');
          onSuccess();
        } else {
          message.error(result?.message || '更新失败');
        }
      } else {
        result = await createApi(data);
        if (result && result.code === '200') {
          message.success('注册成功');
          onSuccess();
        } else {
          message.error(result?.message || '注册失败');
        }
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
            rules={[
              { required: true, message: '请输入API英文名称' },
              { pattern: /^[a-zA-Z0-9\s\-_()]+$/, message: '英文名称不能输入中文字符' }
            ]}
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
              {HTTP_METHOD_OPTIONS.map(opt => (
                <Option value={opt.value}>{opt.label}</Option>
              ))}
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
            rules={[
              { required: true, message: '请输入权限英文名称' },
              { pattern: /^[a-zA-Z0-9\s\-_()]+$/, message: '英文名称不能输入中文字符' }
            ]}
          >
            <Input placeholder="请输入权限英文名称" disabled={mode === 'view'} />
          </Form.Item>

          <Form.Item
            label="Scope标识"
            name="scope"
            rules={[
              { required: true, message: '请输入Scope标识' },
              { pattern: /^api:[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$/, message: '格式不正确，应为：api:{模块}:{资源标识}' }
            ]}
            extra="格式：api:{模块}:{资源标识}"
          >
            <Input placeholder="api:im:send-message" disabled={mode === 'view'} />
          </Form.Item>

          {/* 是否需要审批 */}
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
          propertyPresets={API_PROPERTY_PRESETS}
          title="扩展属性（可选）"
        />
      </Form>
    </Modal>
  );
}

export default ApiRegister;