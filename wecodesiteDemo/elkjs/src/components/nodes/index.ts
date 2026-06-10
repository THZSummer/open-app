import { NodeTypes } from 'reactflow';
import BaseNode from './BaseNode';
import TextNode from './TextNode';
import { NodeType } from '../../types/flow';

/**
 * 注册自定义节点类型
 */
export const nodeTypes: NodeTypes = {
  trigger: BaseNode,
  action: BaseNode,
  end: BaseNode,
  text: TextNode,
  'loop-v2': BaseNode,
  'error-handler': BaseNode,
  parallel: BaseNode
};

/**
 * 默认的节点类型列表
 */
export const defaultNodeTypes = [
  {
    type: NodeType.ACTION,
    label: '动作',
    color: 'var(--node-action)'
  },
  {
    type: NodeType.LOOP_V2,
    label: '循环节点',
    color: 'var(--node-loop)'
  },
  {
    type: NodeType.ERROR_HANDLER,
    label: '错误处理节点',
    color: 'var(--node-loop)'
  },
  {
    type: NodeType.PARALLEL,
    label: '并行处理节点',
    color: 'var(--node-action)'
  }
];
