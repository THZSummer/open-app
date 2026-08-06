import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Select } from 'antd';
import { getSdkPermissions } from '../thunk';

const { TextArea } = Input;

/**
 * SDK版本上架/编辑表单组件
 */
const SdkVersionFormModal = ({
  open,
  title,
  loading,
  editingId,
  initialValues,
  onClose,
  onSubmit
}) => {
  const [form] = Form.useForm();
  const [permissions, setPermissions] = useState([]);

  // 弹窗打开时加载权限列表
  useEffect(() => {
    if (!open) return;
    let active = true;
    const loadPermissions = async () => {
      const result = await getSdkPermissions();
      if (active && result && result.code === '200') {
        setPermissions(result.data || []);
      }
    };
    loadPermissions();
    return () => { active = false; };
  }, [open]);

  // 权限列表加载完成或弹窗打开时设置表单值
  useEffect(() => {
    if (!open) return;
    if (initialValues) {
      form.setFieldsValue({
        updateNotes: initialValues.updateNotes || '',
        artifactUrl: initialValues.artifactUrl || '',
        permissionId: initialValues.permissionId || undefined
      });
    } else {
      form.resetFields();
    }
  }, [open, initialValues, permissions]);

  const handleOk = () => {
    form.validateFields().then((values) => {
      onSubmit(values);
    });
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      title={title}
      open={open}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      destroyOnClose
      width={500}
    >
      <Form form={form} layout="vertical">
        {!editingId && (
          <Form.Item
            name="versionName"
            label="版本号"
            rules={[
              { required: true, message: '请输入版本号' },
              { max: 64, message: '版本号长度不能超过64字符' }
            ]}
          >
            <Input placeholder="如 1.3.0" />
          </Form.Item>
        )}

        <Form.Item
          name="permissionId"
          label="关联权限"
        >
          <Select
            placeholder="请选择关联权限"
            allowClear
            options={permissions.map(p => ({
              value: p.id,
              label: `${p.nameCn}（${p.scope}）`
            }))}
          />
        </Form.Item>

        <Form.Item
          name="updateNotes"
          label="更新说明"
          rules={[{ max: 2000, message: '更新说明长度不能超过2000字符' }]}
        >
          <TextArea rows={4} placeholder="本次更新内容..." showCount maxLength={2000} />
        </Form.Item>

        <Form.Item
          name="artifactUrl"
          label="构建产物下载地址"
          rules={[{ max: 512, message: '地址长度不能超过512字符' }]}
        >
          <Input placeholder="https://artifacts.example.com/xxx.jar" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default SdkVersionFormModal;
