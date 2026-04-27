import React, { useState, useEffect } from 'react';
import { Modal, Input } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';

function DeleteConfirmModal({ 
  open, 
  onClose, 
  onConfirm, 
  title = '确认删除', 
  content = '确定要删除吗？', 
  loading,
  requireConfirmText = null 
}) {
  const [confirmText, setConfirmText] = useState('');
  
  useEffect(() => {
    if (!open) {
      setConfirmText('');
    }
  }, [open]);

  const isConfirmDisabled = loading || (requireConfirmText && confirmText !== requireConfirmText);

  const handleConfirm = () => {
    if (!isConfirmDisabled) {
      onConfirm();
    }
  };

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

      {requireConfirmText && (
        <div style={{ marginBottom: 16, padding: '0 20px' }}>
          <div style={{ marginBottom: 8, color: '#333' }}>
            请输入 <strong style={{ color: '#1677ff' }}>{requireConfirmText}</strong> 以确认删除：
          </div>
          <Input
            placeholder={`请输入 ${requireConfirmText}`}
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            status={confirmText && confirmText !== requireConfirmText ? 'error' : ''}
          />
          {confirmText && confirmText !== requireConfirmText && (
            <div style={{ color: '#ff4d4f', marginTop: 4, fontSize: 12 }}>
              输入不匹配，请重新输入
            </div>
          )}
        </div>
      )}

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
          onClick={handleConfirm}
          disabled={isConfirmDisabled}
          style={{ 
            padding: '6px 24px', 
            background: '#ff4d4f', 
            color: '#fff',
            border: 'none',
            borderRadius: 4,
            cursor: isConfirmDisabled ? 'not-allowed' : 'pointer',
            opacity: isConfirmDisabled ? 0.7 : 1
          }}
        >
          {loading ? '删除中...' : '确认删除'}
        </button>
      </div>
    </Modal>
  );
}

export default DeleteConfirmModal;