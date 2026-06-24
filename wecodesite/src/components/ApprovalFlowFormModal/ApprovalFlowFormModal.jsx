import React, { useEffect } from 'react';
import {
  Modal,
  Form,
  Input,
  Button,
  Divider,
  Space,
  Select,
} from 'antd';
import {
  PlusOutlined,
  MinusCircleOutlined,
  UserOutlined,
} from '@ant-design/icons';

const { Option } = Select;

// 流程代码选项：覆盖现有场景与连接流版本审批
const APPROVAL_FLOW_CODE_OPTIONS = [
  { value: 'global', label: 'global - 全局审批' },
  { value: 'api_register', label: 'api_register - API注册审批' },
  { value: 'event_register', label: 'event_register - 事件注册审批' },
  { value: 'callback_register', label: 'callback_register - 回调注册审批' },
  { value: 'api_permission_apply', label: 'api_permission_apply - API权限申请审批' },
  { value: 'event_permission_apply', label: 'event_permission_apply - 事件权限申请审批' },
  { value: 'callback_permission_apply', label: 'callback_permission_apply - 回调权限申请审批' },
  { value: 'connector_flow', label: 'connector_flow - 连接流版本审批' },
  { value: 'app_version_publish', label: 'app_version_publish - 应用版本审批' },
];

// 仅 connector_flow 流程代码支持展示应用 ID
const APP_ID_ENABLED_CODE = 'connector_flow';

function ApprovalFlowFormModal({
  visible,
  isEditing,
  editingFlow,
  onClose,
  onSubmit,
}) {
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);
  // 当前选中的流程代码（用于控制应用 ID 字段显示与否）
  const [currentCode, setCurrentCode] = React.useState('');

  useEffect(() => {
    if (visible) {
      if (isEditing && editingFlow) {
        form.setFieldsValue({
          nameCn: editingFlow.nameCn,
          nameEn: editingFlow.nameEn,
          code: editingFlow.code,
          appId: editingFlow.appId,
          nodes: editingFlow.nodes || [],
        });
        setCurrentCode(editingFlow.code);
      } else {
        form.resetFields();
        form.setFieldsValue({
          nodes: [],
        });
        setCurrentCode('');
      }
    }
  }, [visible, isEditing, editingFlow, form]);

  /**
   * 流程代码变化时同步本地状态，并在切换为非 connector_flow 时清空应用 ID
   * 参数 value 为选中的流程代码
   */
  const handleCodeChange = (value) => {
    setCurrentCode(value);
    if (value !== APP_ID_ENABLED_CODE) {
      form.setFieldsValue({ appId: undefined });
    }
  };

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);

    const nodes = values.nodes ? values.nodes.map((node, index) => ({
      ...node,
      order: index + 1,
      type: 'approver',
    })) : [];

    // 仅 connector_flow 才提交 appId；其他流程代码不带 appId
    const appId = values.code === APP_ID_ENABLED_CODE ? (values.appId || '') : undefined;

    const data = {
      ...values,
      appId,
      nodes,
    };

    await onSubmit(data);
    setLoading(false);
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
      okText="保存"
      cancelText="取消"
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
            { required: true, message: '请选择流程代码' },
          ]}
          extra="选择审批流程类型，创建后不可修改"
        >
          <Select
            placeholder="请选择流程代码"
            disabled={!!isEditing}
            onChange={handleCodeChange}
          >
            {APPROVAL_FLOW_CODE_OPTIONS.map(opt => (
              <Option key={opt.value} value={opt.value}>
                {opt.label}
              </Option>
            ))}
          </Select>
        </Form.Item>

        {/* 应用 ID：仅 connector_flow 流程代码下展示，非必填；不填为全局审批，填写后为应用级审批 */}
        {currentCode === APP_ID_ENABLED_CODE && (
          <Form.Item
            label="应用 ID"
            name="appId"
            extra="非必填；不填为全局审批，填写后为应用级审批"
          >
            <Input placeholder="请输入应用 ID（不填为全局审批）" allowClear />
          </Form.Item>
        )}

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
            <li>code='connector_flow' 为连接流版本审批：未填写应用 ID 为全局审批，填写应用 ID 为应用级审批</li>
            <li>权限申请流程的资源审批节点从 permission.resource_nodes 字段获取</li>
            <li>流程代码创建后不可修改</li>
          </ul>
        </div>
      </Form>
    </Modal>
  );
}

export default ApprovalFlowFormModal;
