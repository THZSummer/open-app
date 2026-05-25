/**
 * ========================================
 * 引用参数选择器组件
 * ========================================
 *
 * 功能：
 * - 按节点分组展示上游参数
 * - 支持递归平铺展开参数
 * - 支持搜索过滤
 */

import React, { useMemo } from 'react';
import { TreeSelect, Tag, Space } from 'antd';
import { NODE_TYPE_META } from '../../pages/ConnectPlatform/FlowEditor/customNodes';

/**
 * 参数选择器组件
 *
 * @param {Object} props
 * @param {Array} props.upstreamParams - 上游节点参数列表
 * @param {string|undefined} props.value - 当前选中值
 * @param {Function} props.onChange - 值变化回调
 * @param {boolean} props.disabled - 是否禁用
 */
const ParameterSelector = ({
  upstreamParams = [],
  value,
  onChange,
  disabled = false,
}) => {
  /**
   * 转换数据为 TreeSelect 可用的树形结构
   */
  const treeData = useMemo(() => {
    return upstreamParams.map(nodeGroup => ({
      title: nodeGroup.nodeName,
      value: `node_${nodeGroup.nodeId}`,
      nodeType: nodeGroup.nodeType,
      selectable: false,
      children: nodeGroup.params.map(param => ({
        title: param.paramPath,
        value: `${nodeGroup.nodeName}.${param.paramPath}`,
        paramType: param.paramType,
        isLeaf: true,
      })),
    }));
  }, [upstreamParams]);

  /**
   * 过滤树节点
   */
  const filterTreeNode = (input, node) => {
    return node.title.toLowerCase().includes(input.toLowerCase());
  };

  /**
   * 获取节点类型对应的颜色
   */
  const getNodeTypeColor = (nodeType) => {
    return NODE_TYPE_META[nodeType]?.color || '#999';
  };

  /**
   * 获取选中参数所属的节点类型
   */
  const getSelectedNodeType = () => {
    if (!value) return null;
    const nodeGroup = upstreamParams.find(n => value.startsWith(n.nodeName + '.'));
    return nodeGroup?.nodeType;
  };

  const selectedNodeType = getSelectedNodeType();

  return (
    <TreeSelect
      value={value}
      onChange={onChange}
      placeholder="选择引用参数"
      disabled={disabled}
      showSearch
      allowClear
      treeData={treeData}
      filterTreeNode={filterTreeNode}
      treeDefaultExpandAll
      style={{ width: '100%' }}
      dropdownStyle={{ maxHeight: 300, overflow: 'auto' }}
      treeNodeLabelProp="title"
      suffixIcon={
        value && selectedNodeType ? (
          <Space size={4}>
            <Tag color={getNodeTypeColor(selectedNodeType)} style={{ margin: 0 }}>
              引用
            </Tag>
          </Space>
        ) : null
      }
    />
  );
};

export default ParameterSelector;
