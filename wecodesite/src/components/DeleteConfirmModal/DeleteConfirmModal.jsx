import React, { useState, useEffect } from 'react';
import { Modal, Input } from 'antd';


function DeleteConfirmModal({
  open,
  onClose,
  onConfirm,
  modalInfo,
  loading,
  requireConfirmText = null
}) {
  const [confirmText, setConfirmText] = useState('');

  useEffect(() => {
    if (!open) {
      setConfirmText('');
    }
  }, [open]);

  const isConfirmDisabled = () => {
    if (requireConfirmText) {
      return loading || confirmText !== requireConfirmText;
    } else {
      return loading;
    }
  };

  const handleConfirm = () => {
    if (!isConfirmDisabled()) {
      onConfirm();
    }
  };

  // 兼容 content 为字符串（主调方传整段文案）或对象（{confirmText, impactText}）
  const rawContent = modalInfo?.content;
  const confirmTextLine = typeof rawContent === 'string' ? rawContent : (rawContent?.confirmText || '');
  const impactTextLine = (rawContent && typeof rawContent === 'object') ? (rawContent.impactText || '') : '';

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

        <div style={{ color: '#8c8c8c', textAlign: 'left' }}>
          {confirmTextLine ? <div style={{ marginBottom: 8 }}>{confirmTextLine}</div> : null}
          {impactTextLine ? <div>{impactTextLine}</div> : null}
        </div>
      </div>

      {requireConfirmText && (
        <div style={{ marginBottom: 16, padding: '0 20px' }}>
          <div style={{ marginBottom: 8, color: '#333' }}>
            请输入 <strong style={{ color: '#1677ff' }}>{requireConfirmText}</strong> 以确认：
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
          disabled={isConfirmDisabled()}
          style={{
            padding: '6px 24px',
            background: modalInfo.dangerColor,
            color: '#fff',
            border: 'none',
            borderRadius: 4,
            cursor: isConfirmDisabled() ? 'not-allowed' : 'pointer',
            opacity: isConfirmDisabled() ? 0.7 : 1
          }}
        >
          {loading ? modalInfo.loadingText : modalInfo.confirmButtonText}
        </button>
      </div>
    </Modal>
  );
}

export default DeleteConfirmModal;
