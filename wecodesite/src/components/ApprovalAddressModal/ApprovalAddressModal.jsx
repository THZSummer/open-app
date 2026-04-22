import React, { useState } from 'react';
import { Modal, Button, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';

function ApprovalAddressModal({ open, onClose, approver, approvalUrl, onRemind }) {
  const [loading, setLoading] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(approvalUrl).then(() => {
      message.success('审批链接已复制');
    });
  };

  const handleRemind = async () => {
    setLoading(true);
    try {
      await onRemind();
      message.success('催办成功');
      onClose();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="复制审批地址"
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="cancel" onClick={onClose}>取消</Button>,
        <Button key="remind" type="primary" loading={loading} onClick={handleRemind}>
          催办
        </Button>
      ]}
      centered
      width={400}
    >
      <div style={{ padding: '16px 0' }}>
        <div style={{ marginBottom: 16 }}>
          <span style={{ color: '#8c8c8c' }}>审批人：</span>
          <span>{approver}</span>
        </div>
        <div>
          <span style={{ color: '#8c8c8c' }}>审批链接：</span>
          <span style={{ 
            display: 'inline-block',
            maxWidth: 200,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            verticalAlign: 'bottom'
          }}>
            {approvalUrl}
          </span>
          <Button 
            type="link" 
            icon={<CopyOutlined />} 
            onClick={handleCopy}
            size="small"
          />
        </div>
      </div>
    </Modal>
  );
}

export default ApprovalAddressModal;