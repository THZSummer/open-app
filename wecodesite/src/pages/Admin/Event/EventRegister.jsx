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
  Radio,
  message,
} from 'antd';
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons';
import { createEvent, updateEvent, fetchEventDetail } from './thunk';
import { fetchCategoryTree } from '../Category/thunk';
import { EVENT_PROPERTY_PRESETS } from './constants';

const { Option } = Select;

function EventRegister({ visible, event, mode = 'create', onSuccess, onCancel }) {
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
    const loadEventDetail = async () => {
      if (visible && event?.id) {
        setLoading(true);
        try {
          const result = await fetchEventDetail(event.id);
          if (result.code === '200') {
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
              // 解析 resourceNodes JSON 字符串为数组
              resourceNodes: data.permission?.resourceNodes 
                ? JSON.parse(data.permission.resourceNodes) 
                : [],
              properties: data.properties?.map(prop => ({
                propertyName: prop.propertyName,
                propertyValue: prop.propertyValue,
                customPropertyName: EVENT_PROPERTY_PRESETS.find(p => p.value === prop.propertyName) 
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

    loadEventDetail();
  }, [visible, event, form]);

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
        topic: values.topic,
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
      if (event?.id) {
        result = await updateEvent(event.id, data);
        if (result && result.code === '200') {
          message.success('更新成功');
          onSuccess();
        } else {
          message.error(result?.message || '更新失败');
        }
      } else {
        result = await createEvent(data);
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
        mode === 'view' ? '查看事件详情' :
        mode === 'edit' ? '编辑事件' :
        '注册事件'
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
            label="中文名称"
            name="nameCn"
            rules={[{ required: true, message: '请输入事件中文名称' }]}
          >
            <Input placeholder="请输入事件中文名称" disabled={mode === 'view'} />
          </Form.Item>

          <Form.Item
            label="英文名称"
            name="nameEn"
            rules={[
              { required: true, message: '请输入事件英文名称' },
              { pattern: /^[a-zA-Z0-9\s\-_()]+$/, message: '英文名称不能输入中文字符' }
            ]}
          >
            <Input placeholder="请输入事件英文名称" disabled={mode === 'view'} />
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
            label="Topic 标识"
            name="topic"
            rules={[
              { required: true, message: '请输入 Topic 标识' },
              { pattern: /^[a-zA-Z][a-zA-Z0-9]*\.[a-zA-Z][a-zA-Z0-9.]*/, message: '格式不正确，应为：模块.事件' },
            ]}
            extra="格式：模块.事件，例如：user.status, msg.send.ok"
          >
            <Input placeholder="user.status" disabled={mode === 'view'} />
          </Form.Item>
        </Card>

        <Card title="权限信息" size="small" style={{ marginBottom: 16 }}>
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
              { pattern: /^event:[a-zA-Z0-9_-]+:[a-zA-Z0-9_-]+$/, message: '格式不正确，应为：event:{模块}:{事件标识}' }
            ]}
            extra="格式：event:{模块}:{事件标识}"
          >
            <Input placeholder="event:im:message-received" disabled={mode === 'view'} />
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

          {/* 审批节点配置（当需要审批时显示） */}
          <Form.Item noStyle shouldUpdate={(prev, cur) => prev.needApproval !== cur.needApproval}>
            {({ getFieldValue }) => {
              const needApproval = getFieldValue('needApproval');
              if (needApproval === 1) {
                return (
                  <Form.Item label="审批节点配置" required>
                    <Form.List name="resourceNodes">
                      {(fields, { add, remove }) => (
                        <>
                          {fields.map(({ key, name, ...restField }) => (
                            <Space key={key} style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                              <Form.Item
                                {...restField}
                                name={[name, 'userId']}
                                rules={[{ required: true, message: '请输入审批人ID' }]}
                              >
                                <Input placeholder="审批人用户ID" style={{ width: 150 }} disabled={mode === 'view'} />
                              </Form.Item>
                              <Form.Item
                                {...restField}
                                name={[name, 'userName']}
                                rules={[{ required: true, message: '请输入审批人姓名' }]}
                              >
                                <Input placeholder="审批人姓名" style={{ width: 150 }} disabled={mode === 'view'} />
                              </Form.Item>
                              <Form.Item
                                {...restField}
                                name={[name, 'order']}
                                rules={[{ required: true, message: '请输入审批顺序' }]}
                              >
                                <Input placeholder="审批顺序" style={{ width: 80 }} disabled={mode === 'view'} />
                              </Form.Item>
                              {mode !== 'view' && <MinusCircleOutlined onClick={() => remove(name)} />}
                            </Space>
                          ))}
                          {mode !== 'view' && (
                            <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined />}>
                              添加审批节点
                            </Button>
                          )}
                        </>
                      )}
                    </Form.List>
                  </Form.Item>
                );
              }
              return null;
            }}
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
                          disabled={mode === 'view'}
                          onChange={(value) => {
                            const properties = form.getFieldValue('properties');
                            properties[name].propertyValue = undefined;
                            form.setFieldsValue({ properties });
                          }}
                        >
                          {EVENT_PROPERTY_PRESETS.map(preset => (
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
                                <Input placeholder="自定义属性名" style={{ width: 140 }} disabled={mode === 'view'} />
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
                          const preset = EVENT_PROPERTY_PRESETS.find(p => p.value === propertyName);
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

export default EventRegister;
