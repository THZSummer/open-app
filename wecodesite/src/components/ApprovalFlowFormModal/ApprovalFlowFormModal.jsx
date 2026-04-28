import React, { useEffect } from 'react';
import {
  Modal,
  Form,
  Input,
  Button,
  Divider,
  Tag,
  Space,
} from 'antd';
import {
  MinusCircleOutlined,
  PlusOutlined,
  UserOutlined,
} from '@ant-design/icons';

function ApprovalFlowFormModal({
  visible,
  isEditing,
  editingFlow,
  onClose,
  onSubmit,
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);

  useEffect(() => {
    if (visible) {
      if (isEditing && editingFlow) {
        form.setFieldsValue({
          nameCn: editingFlow.nameCn,
          nameEn: editingFlow.nameEn,
          code: editingFlow.code,
          nodes: editingFlow.nodes || [],
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          nodes: [],
        });
      }
    }
  }, [visible, isEditing, editingFlow, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const nodes = values.nodes ? values.nodes.map((node, index) => ({
        ...node,
        order: index + 1,
        type: 'approver',
      })) : [];

      const data = {
        ...values,
        nodes,
      };

      await onSubmit(data);
      form.resetFields();
    } catch (error) {
      console.error('Validation failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      title={isEditing ? '编辑审批流程' : '新建审批流程'}
      open={visible}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={700}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
      >
        <Form.Item
          label="流程名称（中文）"
          name="nameCn"
          rules={[{ required: true, message: '请输入流程名称（中文）' }]}
        >
          <Input placeholder="请输入流程名称（中文）" />
        </Form.Item>

        <Form.Item
          label="流程名称（英文）"
          name="nameEn"
          rules={[{ required: true, message: '请输入流程名称（英文）' }]}
        >
          <Input placeholder="请输入流程名称（英文）" />
        </Form.Item>

        <Form.Item
          label="流程代码"
          name="code"
          rules={[
            { required: true, message: '请输入流程代码' },
            { pattern: /^[a-z_]+$/, message: '流程代码只能包含小写字母和下划线' },
          ]}
          extra="常用代码：global（全局审批）、api_register、event_register、callback_register、api_permission_apply、event_permission_apply、callback_permission_apply"
        >
          <Input
            placeholder="请输入流程代码（如：api_register）"
            disabled={!!isEditing}
          />
        </Form.Item>

        <Divider orientation="left">审批节点配置</Divider>

        <Form.List name="nodes">
          {(fields, { add, remove }) => (
            <>
              {fields.length === 0 && (
                <div style={{
                  padding: 24,
                  textAlign: 'center',
                  color: '#999',
                  border: '1px dashed #d9d9d9',
                  borderRadius: 4,
                  marginBottom: 16,
                }}>
                  <p style={{ marginBottom: 8 }}>暂无审批节点</p>
                  <p style={{ marginBottom: 0, fontSize: 12 }}>
                    点击下方"添加审批节点"按钮添加审批人
                  </p>
                </div>
              )}

              {fields.map(({ key, name, ...restField }, index) => (
                <div
                  key={key}
                  style={{
                    padding: 16,
                    marginBottom: 16,
                    border: '1px solid #d9d9d9',
                    borderRadius: 4,
                    background: '#fafafa',
                  }}
                >
                  <div style={{ marginBottom: 8, fontWeight: 'bold' }}>
                    <UserOutlined style={{ marginRight: 8 }} />
                    审批节点 {index + 1}
                  </div>

                  <Space style={{ display: 'flex', marginBottom: 8 }} align="baseline">
                    <Form.Item
                      {...restField}
                      name={[name, 'userId']}
                      rules={[{ required: true, message: '请输入审批人ID' }]}
                      style={{ marginBottom: 0, flex: 1 }}
                    >
                      <Input placeholder="审批人ID" />
                    </Form.Item>

                    <Form.Item
                      {...restField}
                      name={[name, 'userName']}
                      rules={[{ required: true, message: '请输入审批人姓名' }]}
                      style={{ marginBottom: 0, flex: 1 }}
                    >
                      <Input placeholder="审批人姓名" />
                    </Form.Item>

                    <MinusCircleOutlined
                      onClick={() => remove(name)}
                      style={{ fontSize: 20, color: '#ff4d4f' }}
                    />
                  </Space>
                </div>
              ))}

              <Button
                type="dashed"
                onClick={() => add({ type: 'approver', order: fields.length + 1 })}
                block
                icon={<PlusOutlined />}
              >
                添加审批节点
              </Button>
            </>
          )}
        </Form.List>

        <div style={{ marginTop: 16, color: '#666', fontSize: 12 }}>
          <p>提示：</p>
          <ul style={{ paddingLeft: 20 }}>
            <li>审批节点按顺序执行，序号从1开始递增</li>
            <li>code='global' 为全局审批流程，其他为场景审批流程</li>
            <li>权限申请流程的资源审批节点从 permission.resource_nodes 字段获取</li>
            <li>流程代码创建后不可修改</li>
          </ul>
        </div>
      </Form>
    </Modal>
  );
}

export default ApprovalFlowFormModal;
