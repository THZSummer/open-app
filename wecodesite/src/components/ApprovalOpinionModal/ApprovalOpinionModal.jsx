import React, { useState } from 'react';
import { Modal, Input, Typography } from 'antd';

const { TextArea } = Input;
const { Text } = Typography;

function ApprovalOpinionModal({
  visible,
  title = '审批意见',
  okText = '确认',
  okButtonProps = {},
  isRequired = false,
  approvalId,
  onClose,
  onConfirm,
}) {
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleConfirm = async () => {
    if (isRequired && !comment.trim()) {
      setError('请输入审批意见');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await onConfirm(approvalId, comment.trim());
      setComment('');
    } catch (err) {
      setError('操作失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setComment('');
    setError('');
    onClose();
  };

  return (
    <Modal
      title={title}
      open={visible}
      onCancel={handleClose}
      onOk={handleConfirm}
      okText={okText}
      confirmLoading={loading}
      okButtonProps={okButtonProps}
    >
      <div style={{ marginBottom: 8 }}>
        <Text type="secondary">
          {isRequired ? '请输入审批意见：' : '审批意见（可选）：'}
        </Text>
      </div>
      <TextArea
        rows={4}
        value={comment}
        onChange={(e) => {
          setComment(e.target.value);
          if (error) setError('');
        }}
        placeholder="请输入审批意见"
        maxLength={500}
        showCount
        status={error ? 'error' : ''}
      />
      {error && (
        <div style={{ color: '#ff4d4f', marginTop: 4, fontSize: 12 }}>
          {error}
        </div>
      )}
    </Modal>
  );
}

export default ApprovalOpinionModal;
