import React, { useState } from 'react';
import { Modal, Button, message } from 'antd';
import { CopyOutlined } from '@ant-design/icons';
import { remindPeople } from '../../pages/Admin/Approval/thunk';

/**
 * 从审批链接中提取 eflowId
 * @param {string} url - 审批地址URL
 * @returns {string} eflowId 值或空字符串
 */
const extractEflowId = (url) => {
  if (!url) return '';
  
  const match = url.match(/eflowId=([^&]+)/);
  return match ? match[1] : '';
};

function ApprovalAddressModal({ open, onClose, approver, approvalUrl }) {
  const [remindLoading, setRemindLoading] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(approvalUrl).then(() => {
      message.success('审批链接已复制');
    }).catch(() => message.error('复制失败，请检查浏览器权限'));
  };

  /**
   * 处理催办按钮点击事件
   * 从审批链接中提取 eflowId 并调用催办接口
   */
  const handleRemind = async () => {
    const eflowId = extractEflowId(approvalUrl);
    
    if (!eflowId) {
      message.error('无法提取审批ID');
      return;
    }
    
    setRemindLoading(true);
    
    const result = await remindPeople({ eflowId });
    
    if (result && result.code === '200') {
      message.success('催办成功');
      onClose();
    } else {
      message.error(result.messageZh || '催办失败');
    }
    
    setRemindLoading(false);
  };

  return (
    <Modal
      title="复制审批地址"
      open={open}
      onCancel={onClose}
      footer={[
        <Button
          key="remind"
          type="primary"
          onClick={handleRemind}
          loading={remindLoading}
        >
          催办
        </Button>,
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
