/**
 * ========================================
 * 自定义流程节点注册
 * ========================================
 * 
 * 统一注册所有自定义节点类型，供FlowCanvas使用
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
 */
export const NODE_LIBRARY = [
  {
    category: '触发器',
    icon: '⚡',
    items: [
      { type: 'trigger', label: '定时触发', description: '按照Cron表达式定时执行' },
      { type: 'trigger', label: 'Webhook', description: '通过HTTP回调触发' },
      { type: 'trigger', label: 'API触发', description: '通过API接口触发' },
    ],
  },
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
