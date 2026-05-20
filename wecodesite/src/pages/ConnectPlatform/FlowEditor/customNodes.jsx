/**
 * ========================================
 * 自定义流程节点注册
 * ========================================
 *
 * 统一注册所有自定义节点类型，供FlowEditor使用
 */

import TriggerNode from '../../../components/CustomFlowNodes/TriggerNode';
import ActionNode from '../../../components/CustomFlowNodes/ActionNode';
import ConditionNode from '../../../components/CustomFlowNodes/ConditionNode';
import DelayNode from '../../../components/CustomFlowNodes/DelayNode';
import ParallelNode from '../../../components/CustomFlowNodes/ParallelNode';
import LoopNode from '../../../components/CustomFlowNodes/LoopNode';

/**
 * 节点类型注册表
 * 格式：{ typeName: Component }
 */
export const nodeTypes = {
  trigger: TriggerNode,
  action: ActionNode,
  condition: ConditionNode,
  delay: DelayNode,
  parallel: ParallelNode,
  loop: LoopNode,
};

/**
 * 节点库配置
 * 定义可在节点库面板中拖拽的节点
 * 注意：触发器节点不在此配置中，因为每个流程只能有一个触发器
 */
export const NODE_LIBRARY = [
  {
    category: '执行动作',
    icon: '▶',
    items: [
      { type: 'action', label: '执行动作', description: '调用连接器的执行动作' },
    ],
  },
  {
    category: '逻辑控制',
    icon: '🔀',
    items: [
      { type: 'condition', label: '条件分支', description: '根据条件进行分支判断' },
      { type: 'delay', label: '延时', description: '等待指定时间后继续' },
      { type: 'parallel', label: '并行执行', description: '同时执行多个分支' },
      { type: 'loop', label: '循环执行', description: '循环执行指定次数' },
    ],
  },
];

/**
 * 节点类型元数据
 * 用于显示和配置
 */
export const NODE_TYPE_META = {
  trigger: {
    name: '触发器',
    color: '#1890ff',
    description: '定义流程的触发方式',
  },
  action: {
    name: '执行动作',
    color: '#52c41a',
    description: '调用连接器的执行动作',
  },
  condition: {
    name: '条件分支',
    color: '#faad14',
    description: '根据条件进行分支判断',
  },
  delay: {
    name: '延时',
    color: '#722ed1',
    description: '等待指定时间后继续',
  },
  parallel: {
    name: '并行执行',
    color: '#13c2c2',
    description: '同时执行多个分支',
  },
  loop: {
    name: '循环执行',
    color: '#eb2f96',
    description: '循环执行指定次数',
  },
};
