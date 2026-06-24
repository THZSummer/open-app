/**
 * ========================================
 * 连接器/连接流表单弹窗组件
 * ========================================
 *
 * 功能：
 * - 新增连接器/连接流表单
 * - 编辑连接器/连接流表单
 */

import React, { useEffect } from 'react';
import { Modal, Form, Input, Select, message } from 'antd';

const { TextArea } = Input;

/**
 * 连接器类型选项
 */
const connectorTypeOptions = [
  { value: 1, label: 'HTTP' },
];

/**
 * 连接器/连接流表单弹窗组件
 *
 * @param {Object} props - 组件属性
 * @param {string} props.type - 表单类型：'connector' | 'flow'
 * @param {boolean} props.visible - 弹窗是否显示
 * @param {Function} props.onCancel - 取消回调
 * @param {Function} props.onOk - 确认回调
 * @param {Object} props.initialValues - 初始值（编辑时使用）
 * @param {boolean} props.loading - 提交loading状态
 */
function ConnectorFormModal({ type = 'connector', visible, onCancel, onOk, initialValues = null, loading = false }) {
  const [form] = Form.useForm();
  const isEdit = !!initialValues;
  const isConnector = type === 'connector';

  useEffect(() => {
    if (visible) {
      form.resetFields();
      if (initialValues) {
        form.setFieldsValue(initialValues);
      } else if (isConnector) {
        form.setFieldsValue({
          connectorType: 1,
        });
      }
    }
  }, [visible, initialValues, form, isConnector]);

  /**
   * 获取弹窗标题
   */
  const getModalTitle = () => {
    if (isConnector) {
      return isEdit ? '编辑连接器' : '新建连接器';
    }
    return isEdit ? '编辑连接流' : '新建连接流';
  };

  /**
   * 提交表单
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      onOk(values);
    } catch (error) {
      message.error('请检查表单填写是否正确');
    }
  };

  return (
    <Modal
      title={getModalTitle()}
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText="保存"
      cancelText="取消"
      width={520}
      destroyOnClose
      confirmLoading={loading}
    >
      <Form
        form={form}
        layout="vertical"
      >
        {isConnector && (
          <Form.Item
            name="connectorType"
            label="类型"
            rules={[
              { required: true, message: '请选择连接器类型' }
            ]}
          >
            <Select
              placeholder="请选择连接器类型"
              options={connectorTypeOptions}
              style={{ width: '100%' }}
            />
          </Form.Item>
        )}

        <Form.Item
          name="nameCn"
          label="中文名称"
          rules={[
            { required: true, message: '请输入中文名称' },
            { max: 128, message: '中文名称不能超过128个字符' }
          ]}
        >
          <Input
            placeholder="请输入中文名称"
            maxLength={128}
            showCount
          />
        </Form.Item>

        <Form.Item
          name="nameEn"
          label="英文名称"
          rules={[
            { required: true, message: '请输入英文名称' },
            { max: 128, message: '英文名称不能超过128个字符' }
          ]}
        >
          <Input
            placeholder="请输入英文名称"
            maxLength={128}
            showCount
          />
        </Form.Item>

        <Form.Item
          name="descriptionCn"
          label="中文描述"
          rules={[
            { max: 500, message: '中文描述不能超过500个字符' }
          ]}
        >
          <TextArea
            placeholder="请输入中文描述"
            maxLength={500}
            showCount
            rows={4}
          />
        </Form.Item>

        <Form.Item
          name="descriptionEn"
          label="英文描述"
          rules={[
            { max: 512, message: '英文描述不能超过512个字符' }
          ]}
        >
          <TextArea
            placeholder="请输入英文描述"
            maxLength={512}
            showCount
            rows={4}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}

export default ConnectorFormModal;
