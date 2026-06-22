import React, { useState, useEffect, useRef } from 'react';
import { Modal, Input, Form } from 'antd';
import less from '../index.module.less';

/**
 * 绑定机器人账号弹窗
 *
 * <p>前端仅做非空校验，有效性由后端通过通讯录 API 校验</p>
 *
 * @param {Object} props
 * @param {boolean} props.visible - 是否显示
 * @param {Function} props.onOk - 确认回调，参数为 accountId
 * @param {Function} props.onCancel - 取消回调
 */
const BindAccountModal = ({ visible, onOk, onCancel }) => {
  const [form] = Form.useForm();
  const inputRef = useRef(null);

  useEffect(() => {
    if (visible) {
      form.resetFields();
      // 自动聚焦
      setTimeout(() => {
        inputRef.current?.focus();
      }, 100);
    }
  }, [visible, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      onOk(values.accountId.trim());
    } catch (err) {
      // 校验失败，不关闭弹窗
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  return (
    <Modal
      title="绑定机器人账号"
      open={visible}
      onOk={handleOk}
      onCancel={handleCancel}
      okText="确认"
      cancelText="取消"
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="accountId"
          label="机器人账号"
          rules={[
            { required: true, message: '请输入机器人账号' },
          ]}
        >
          <Input
            ref={inputRef}
            placeholder="输入机器人账号 ID"
            autoComplete="off"
            className={less.monoInput}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default BindAccountModal;
