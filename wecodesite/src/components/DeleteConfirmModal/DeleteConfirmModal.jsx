import React from 'react';
import { Modal } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';

function DeleteConfirmModal({ open, onClose, onConfirm, title = '确认删除', content = '确定要删除吗？', loading }) {
  return (
    <Modal
      title={null}
      open={open}
      onCancel={onClose}
      footer={null}
      centered
      width={400}
    >
      <div style={{ textAlign: 'center', padding: '20px 0' }}>
        <ExclamationCircleOutlined style={{ fontSize: 48, color: '#faad14', marginBottom: 16 }} />
        <div style={{ fontSize: 16, marginBottom: 8 }}>{title}</div>
        <div style={{ color: '#8c8c8c' }}>{content}</div>
      </div>
      <div style={{ textAlign: 'center', paddingBottom: 16 }}>
        <button 
          onClick={onClose}
          style={{ 
            marginRight: 8, 
            padding: '6px 24px', 
            border: '1px solid #d9d9d9', 
            background: '#fff',
            borderRadius: 4,
            cursor: 'pointer'
          }}
        >
          取消
        </button>
        <button 
          onClick={onConfirm}
          disabled={loading}
          style={{ 
            padding: '6px 24px', 
            background: '#ff4d4f', 
            color: '#fff',
            border: 'none',
            borderRadius: 4,
            cursor: loading ? 'not-allowed' : 'pointer',
            opacity: loading ? 0.7 : 1
          }}
        >
          {loading ? '删除中...' : '确认删除'}
        </button>
      </div>
    </Modal>
  );
}

export default DeleteConfirmModal;