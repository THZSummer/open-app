import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Button, Table, Spin, message } from 'antd';
import { getOwnerColumns } from '../../pages/Admin/Category/constants';

function CategoryOwnerModal({
  visible,
  categoryId,
  onClose,
  fetchOwners,
  addOwner,
  removeOwner,
}) {
  const [ownerForm] = Form.useForm();
  const [owners, setOwners] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (visible && categoryId) {
      loadOwners();
    }
  }, [visible, categoryId]);

  const loadOwners = async () => {
    setLoading(true);
    const result = await fetchOwners(categoryId);
    if (result && result.code === '200') {
      setOwners(result.data || []);
    } else {
      message.error(result?.message || '加载责任人失败');
    }
    setLoading(false);
  };

  const handleAddOwner = async () => {
    try {
      const values = await ownerForm.validateFields();
      const res = await addOwner(categoryId, {
        userId: values.userId,
        userName: values.userName,
      });
      if (res && res.code === '200') {
        message.success('添加成功');
        ownerForm.resetFields();
        await loadOwners();
      } else {
        message.error(res?.message || '添加失败');
      }
    } catch (error) {
      message.error('添加失败');
      console.error('Failed to add owner:', error);
    }
  };

  const handleRemoveOwner = async (userId) => {
    try {
      const res = await removeOwner(categoryId, userId);
      if (res && res.code === '200') {
        message.success('移除成功');
        await loadOwners();
      } else {
        message.error(res?.message || '移除失败');
      }
    } catch (error) {
      message.error('移除失败');
    }
  };

  const handleClose = () => {
    ownerForm.resetFields();
    setOwners([]);
    onClose();
  };

  const columns = getOwnerColumns(handleRemoveOwner);

  return (
    <Modal
      title="管理责任人"
      open={visible}
      onCancel={handleClose}
      footer={null}
      width={800}
    >
      <Spin spinning={loading}>
        <div className="owner-section">
          <div className="section-header" style={{ marginBottom: 16 }}>
            <h4>添加责任人</h4>
          </div>
          <Form form={ownerForm} layout="inline">
            <Form.Item
              name="userId"
              label="用户ID"
              rules={[{ required: true, message: '请输入用户ID' }]}
            >
              <Input placeholder="请输入用户ID" style={{ width: 200 }} />
            </Form.Item>
            <Form.Item name="userName" label="用户名称">
              <Input placeholder="请输入用户名称" style={{ width: 200 }} />
            </Form.Item>
            <Form.Item>
              <Button type="primary" onClick={handleAddOwner}>
                添加
              </Button>
            </Form.Item>
          </Form>

          <div className="section-header" style={{ marginTop: 16, marginBottom: 16 }}>
            <h4>责任人列表</h4>
          </div>
          <Table
            dataSource={owners}
            columns={columns}
            rowKey="id"
            pagination={false}
            size="small"
          />
        </div>
      </Spin>
    </Modal>
  );
}

export default CategoryOwnerModal;
