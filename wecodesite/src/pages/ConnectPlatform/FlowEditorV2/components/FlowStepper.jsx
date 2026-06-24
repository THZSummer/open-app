import React from 'react';
import { Tooltip } from 'antd';
import { PlusOutlined, CloseOutlined } from '@ant-design/icons';

/**
 * 步骤条组件
 * 展示节点序列、激活态切换、加号插入和节点删除入口
 *
 * @param {Object} props
 * @param {Array} props.nodeMeta 节点元数据数组 [{ node, title, index }]
 * @param {string} props.activeId 当前激活节点 ID
 * @param {boolean} props.editable 是否可编辑
 * @param {boolean} props.showAddBtn 是否显示加号按钮（单节点模式为 false）
 * @param {Function} props.onSelect 节点切换回调 (nodeId) => void
 * @param {Function} props.onAddNode 加号点击回调 (insertIndex) => void
 * @param {Function} props.onRemoveNode 删除节点回调 (node) => void
 * @param {Function} props.canDeleteNode 判断节点是否可删除 (node) => boolean
 * @param {Function} props.canInsertAt 判断指定位置是否有可插入节点 (insertIndex) => boolean
 */
const FlowStepper = (props) => {
  const {
    nodeMeta,
    activeId,
    editable,
    showAddBtn,
    onSelect,
    onAddNode,
    onRemoveNode,
    canDeleteNode,
    canInsertAt,
  } = props;

  /**
   * 处理节点点击切换激活
   * @param {string} nodeId 节点 ID
   */
  const handleSelect = (nodeId) => {
    if (nodeId === activeId) return;
    onSelect(nodeId);
  };

  /**
   * 处理节点删除点击
   *
   * @param {Object} params
   * @param {Object} params.event React 事件对象
   * @param {Object} params.node 节点对象
   */
  const handleRemove = (params) => {
    // params.event / params.node
    const { event, node } = params;
    event.stopPropagation();
    onRemoveNode(node);
  };

  /**
   * 处理加号点击
   * @param {number} insertIndex 插入位置索引
   */
  const handleAdd = (insertIndex) => {
    onAddNode(insertIndex);
  };

  /**
   * 渲染节点按钮
   *
   * @param {Object} params
   * @param {Object} params.meta 节点元数据
   * @param {number} params.idx 当前在 nodeMeta 中的索引
   */
  const renderNodeItem = (params) => {
    // params.meta / params.idx
    const { meta, idx } = params;
    const { node, title } = meta;
    const isActive = node.id === activeId;
    const deletable = editable && canDeleteNode(node);

    return (
      <div
        key={node.id}
        className={`step-item ${isActive ? 'active' : ''}`}
        onClick={() => handleSelect(node.id)}
        style={{ position: 'relative' }}
      >
        <span className="step-index">{idx + 1}</span>
        <span>{title}</span>
        {deletable && (
          <Tooltip title="删除节点">
            <CloseOutlined
              onClick={(event) => handleRemove({ event, node })}
              style={{
                marginLeft: 4,
                fontSize: 11,
                color: isActive ? '#fff' : '#86909c',
                cursor: 'pointer',
              }}
            />
          </Tooltip>
        )}
      </div>
    );
  };

  return (
    <div className="flow-stepper">
      {nodeMeta.map((meta, idx) => {
        const isLast = idx === nodeMeta.length - 1;
        // 当前加号位置是否存在可插入的节点类型
        const insertable = !isLast && (!canInsertAt || canInsertAt(idx));
        return (
          <React.Fragment key={meta.node.id}>
            {renderNodeItem({ meta, idx })}
            {!isLast && (
              <>
                <div className="step-divider" />
                {editable && showAddBtn && insertable && (
                  <>
                    <Tooltip title="添加节点">
                      <span className="add-step-btn" onClick={() => handleAdd(idx)}>
                        <PlusOutlined style={{ fontSize: 12 }} />
                      </span>
                    </Tooltip>
                    <div className="step-divider" />
                  </>
                )}
              </>
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
};

export default FlowStepper;
