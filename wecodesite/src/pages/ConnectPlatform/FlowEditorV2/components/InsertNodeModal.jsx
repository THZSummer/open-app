import React from 'react';
import { Modal } from 'antd';
import { NODE_TYPE_LABEL } from '../constants';

/**
 * 节点类型对应的图标与描述
 */
const NODE_INSERT_META = {
  connector: { icon: '🔌', desc: '调用连接器接口，支持版本选择和入参映射' },
  script: { icon: '⚙️', desc: 'TypeScript 脚本处理上游数据' },
  parallel: { icon: '∥', desc: '并行执行多个分支' },
};

/**
 * 添加节点弹窗组件
 *
 * @param {Object} props
 * @param {boolean} props.visible 是否显示
 * @param {Array<string>} props.insertableTypes 可插入节点类型列表
 * @param {Function} props.onSelect 选择回调 (type) => void
 * @param {Function} props.onCancel 取消回调
 */
const InsertNodeModal = (props) => {
  const { visible, insertableTypes, onSelect, onCancel } = props;

  /**
   * 处理选择节点
   * @param {string} type 节点类型
   */
  const handleSelect = (type) => {
    onSelect(type);
    onCancel();
  };

  return (
    <Modal
      title="选择要添加的节点"
      open={visible}
      footer={null}
      onCancel={onCancel}
      width={520}
    >
      {(!insertableTypes || insertableTypes.length === 0) ? (
        <div style={{ padding: 24, textAlign: 'center', color: '#86909c' }}>
          当前位置暂无可插入节点
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 8 }}>
          {insertableTypes.map(type => {
            const meta = NODE_INSERT_META[type] || {};
            return (
              <div
                key={type}
                onClick={() => handleSelect(type)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 16,
                  padding: 16,
                  border: '1px solid #e5e6eb',
                  borderRadius: 10,
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                }}
                onMouseEnter={(e) => { e.currentTarget.style.borderColor = '#1677ff'; }}
                onMouseLeave={(e) => { e.currentTarget.style.borderColor = '#e5e6eb'; }}
              >
                <span
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: 10,
                    background: '#e8f3ff',
                    color: '#1677ff',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: 18,
                  }}
                >
                  {meta.icon || '＋'}
                </span>
                <div>
                  <div style={{ fontSize: 14, fontWeight: 600, color: '#1f2329' }}>
                    {NODE_TYPE_LABEL[type] || type}
                  </div>
                  <div style={{ fontSize: 12, color: '#86909c', marginTop: 4 }}>
                    {meta.desc}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </Modal>
  );
};

export default InsertNodeModal;
