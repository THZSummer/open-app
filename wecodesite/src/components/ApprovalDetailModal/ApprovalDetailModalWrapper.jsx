import React from 'react';
import { Modal, Button } from 'antd';
import ApprovalDetailModal from './ApprovalDetailModal';

function ApprovalDetailModalWrapper({
  visible,
  approvalId,
  currentDetail,
  onClose,
  fetchDetail,
}) {
  return (
    <Modal
      title="申请详情"
      open={visible}
      onCancel={onClose}
      footer={
        <Button onClick={onClose}>关闭</Button>
      }
      width={700}
      destroyOnClose
    >
      <ApprovalDetailModal
        visible={visible}
        approvalId={approvalId}
        currentDetail={currentDetail}
        onClose={onClose}
        fetchDetail={fetchDetail}
      />
    </Modal>
  );
}

export default ApprovalDetailModalWrapper;
