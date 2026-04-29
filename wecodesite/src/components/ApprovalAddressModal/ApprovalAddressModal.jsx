import React from 'react';
import { Modal, Button, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';

function ApprovalAddressModal({ open, onClose, approver, approvalUrl }) {
  const handleCopy = () => {
    navigator.clipboard.writeText(approvalUrl).then(() => {
      message.success('审批链接已复制');
    });
  };

  return (
    <Modal
      title="复制审批地址"
      open={open}
      onCancel={onClose}
      footer={[
        <Button key="cancel" onClick={onClose}>关闭</Button>
      ]}
      centered
      width={500}
    >
      <div style={{ padding: '16px 0' }}>
        <div style={{ display: 'flex', marginBottom: 16 }}>
          <span style={{ color: '#8c8c8c', width: 70, flexShrink: 0 }}>审批人：</span>
          <span>{approver}</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'flex-start' }}>
          <span style={{ color: '#8c8c8c', width: 70, flexShrink: 0 }}>审批链接：</span>
          <span style={{ flex: 1, wordBreak: 'break-all' }}>
            {approvalUrl}
          </span>
          <Button
            type="link"
            icon={<CopyOutlined />}
            onClick={handleCopy}
            size="small"
            style={{ marginLeft: 8, flexShrink: 0 }}
          />
        </div>
      </div>
    </Modal>
  );
}

export default ApprovalAddressModal;
