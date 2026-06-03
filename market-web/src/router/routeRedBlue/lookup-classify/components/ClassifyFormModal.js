import React, { useEffect } from 'react';
import { Modal, Form, Input } from 'antd';
import less from './ClassifyFormModal.module.less';
import { FORM_VALIDATION_RULES } from '../constant';

/**
 * 分类表单弹窗组件
 * 提供新增和编辑分类的表单功能
 */
const ClassifyFormModal = ({
  open,
  title,
  loading,
  editingId,
  initialValues,
  onClose,
  onSubmit
}) => {
  const [form] = Form.useForm();
  
  useEffect(() => {
    if (open && initialValues) {
      form.setFieldsValue(initialValues);
    }
  }, [open, initialValues, form]);
  
  const handleOk = () => {
    form.validateFields().then((values) => {
      onSubmit?.(values);
    });
  };
  
  const handleCancel = () => {
    form.resetFields();
    onClose?.();
  };
  
  return (
    <Modal
      title={title}
      open={open}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={520}
      okText="保存"
      cancelText="取消"
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleOk}
      >
        <div className={less.formRow}>
          <Form.Item
            name="classifyCode"
            label="分类编码"
            rules={FORM_VALIDATION_RULES.classifyCode}
            className={less.formItemFlex}
          >
            <Input placeholder="如: USER_TYPE" disabled={!!editingId} />
          </Form.Item>
          <Form.Item
            name="classifyName"
            label="分类名称"
            rules={FORM_VALIDATION_RULES.classifyName}
            className={less.formItemFlex}
          >
            <Input placeholder="如: 用户类型" />
          </Form.Item>
        </div>
        <Form.Item
          name="path"
          label="路径"
          rules={FORM_VALIDATION_RULES.path}
        >
          <Input
            placeholder="如: system/user_type，用于层级归类"
            disabled={!!editingId}
          />
        </Form.Item>
        <div className={less.formHint}>路径可用于分类的层级管理，斜杠分隔</div>
        <Form.Item
          name="classifyDesc"
          label="描述"
          rules={FORM_VALIDATION_RULES.classifyDesc}
        >
          <Input.TextArea
            rows={3}
            placeholder="请输入分类描述..."
            maxLength={4000}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ClassifyFormModal;
