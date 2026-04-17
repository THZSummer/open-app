import React from 'react';
import { Modal, Form, Select } from 'antd';

interface BindEamapModalProps {
  visible: boolean;
  onCancel: () => void;
  onSubmit: (eamapId: string) => void;
  eamapOptions: { value: string; label: string }[];
}

const BindEamapModal: React.FC<BindEamapModalProps> = ({ visible, onCancel, onSubmit, eamapOptions }) => {
  const [form] = Form.useForm();

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      onSubmit(values.eamap);
      form.resetFields();
    });
  };

  return (
    <Modal title="绑定应用服务" open={visible} onOk={handleSubmit} onCancel={onCancel} destroyOnClose>
      <Form form={form} layout="vertical">
        <Form.Item label="选择应用服务" name="eamap" rules={[{ required: true, message: '请选择应用服务' }]}>
          <Select placeholder="请选择应用服务" options={eamapOptions} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default BindEamapModal;
