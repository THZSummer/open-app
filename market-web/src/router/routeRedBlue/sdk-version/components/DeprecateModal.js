import React, { useState, useEffect } from 'react';
import { Modal, Form, Input } from 'antd';

const { TextArea } = Input;

/**
 * SDK版本废弃弹窗组件
 */
const DeprecateModal = ({
  open,
  record,
  loading,
  onClose,
  onConfirm
}) => {
  const [form] = Form.useForm();

  useEffect(() => {
    if (open) {
      form.resetFields();
    }
  }, [open]);

  const handleOk = () => {
    form.validateFields().then((values) => {
      onConfirm(values);
    });
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      title="废弃版本"
      open={open}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      destroyOnClose
      width={460}
    >
      <p style={{ marginBottom: 16, color: '#4e5969' }}>
        确定废弃版本 <strong>{record?.versionName}</strong> 吗？
      </p>
      <Form form={form} layout="vertical">
        <Form.Item
          name="deprecateReason"
          label="废弃原因"
          rules={[
            { required: true, message: '请输入废弃原因' },
            { min: 5, message: '废弃原因至少5个字符' },
            { max: 2000, message: '废弃原因最多2000个字符' }
          ]}
        >
          <TextArea
            rows={4}
            placeholder="必填，5-2000字符，说明废弃原因..."
            showCount
            maxLength={2000}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DeprecateModal;
