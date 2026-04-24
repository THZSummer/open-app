import React, { useState } from 'react';
import { Modal, Input, message } from 'antd';
import { setUserIdCookie } from '../../../utils/cookie';

function LoginModal({ open, onClose, onLoginSuccess }) {
  const [userId, setUserId] = useState('');
  const [loading, setLoading] = useState(false);

  const handleOk = async () => {
    if (!userId.trim()) {
      message.error('请输入用户ID');
      return;
    }
    setLoading(true);
    setUserIdCookie(userId.trim());
    setLoading(false);
    message.success('登录成功');
    setUserId('');
    onLoginSuccess?.(userId.trim());
    onClose();
  };

  const handleCancel = () => {
    setUserId('');
    onClose();
  };

  return (
    <Modal
      title="登录"
      open={open}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      okText="登录"
      cancelText="取消"
    >
      <div style={{ padding: '16px 0' }}>
        <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>用户ID</label>
        <Input
          placeholder="请输入用户ID"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
          onPressEnter={handleOk}
        />
      </div>
    </Modal>
  );
}

export default LoginModal;