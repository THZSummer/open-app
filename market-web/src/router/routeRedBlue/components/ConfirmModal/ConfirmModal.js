import React from 'react';
import { Modal } from 'antd';
import { ExclamationCircleOutlined } from '@ant-design/icons';
import less from './ConfirmModal.module.less';

/**
 * 二次确认弹窗组件
 * 提供通用的确认对话框功能，用于删除、状态切换等危险操作的二次确认
 * 
 * @param {Object} props - 组件属性
 * @param {boolean} props.open - 控制弹窗显示隐藏
 * @param {string} props.title - 弹窗标题
 * @param {string} props.content - 确认内容
 * @param {Function} props.onConfirm - 确认回调函数
 * @param {Function} props.onClose - 关闭回调函数
 * @param {boolean} [props.loading] - 确认按钮加载状态
 */
const ConfirmModal = ({ 
  open, 
  onClose, 
  onConfirm, 
  title = '确认删除', 
  content = '确定要删除吗？', 
  loading = false,
}) => {
  return (
    <Modal
      title={null}
      open={open}
      onCancel={onClose}
      footer={null}
      centered
      width={400}
    >
      <div className={less.confirmModalContent}>
        <ExclamationCircleOutlined className={less.confirmIcon} />
        <div className={less.confirmTitle}>{title}</div>
        <div className={less.confirmText}>{content}</div>
      </div>

      <div className={less.confirmModalFooter}>
        <button 
          className={less.cancelButton}
          onClick={onClose}
        >
          取消
        </button>
        <button 
          className={less.confirmButton}
          onClick={onConfirm}
          disabled={loading}
        >
          {loading ? '处理中...' : '确认'}
        </button>
      </div>
    </Modal>
  );
}

export default ConfirmModal;