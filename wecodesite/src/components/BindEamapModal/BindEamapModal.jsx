import React, { useEffect } from 'react';
import { Modal, Form } from 'antd';
import BindEamapSelect from '../BindEamapSelect/BindEamapSelect';

function BindEamapModal({ visible, onCancel, onOk, appId, eamapOptions = [], currentEamap }) {
  const [form] = Form.useForm();

  useEffect(() => {
    if (visible) {
      form.resetFields();
      form.setFieldsValue({ eamap: currentEamap });
    }
  }, [visible, form, currentEamap]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      onOk(values.eamap);
    } catch (error) {
      console.error('Validation failed:', error);
    }
  };

  return (
    <Modal
      title="绑定到应用服务"
      open={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      okText="确认绑定"
      width={480}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
      >
        <Form.Item
          name="eamap"
          label="选择要绑定的EAMAP"
          rules={[{ required: true, message: '请选择要绑定的EAMAP' }]}
        >
          <BindEamapSelect
            eamapOptions={eamapOptions}
            placeholder="请选择要绑定的EAMAP"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}

export default BindEamapModal;