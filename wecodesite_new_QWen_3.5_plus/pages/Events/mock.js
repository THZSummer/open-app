// Mock 事件配置数据
export const eventList = [
  {
    id: 'event-001',
    name: '用户添加应用事件',
    type: 'app_user_add',
    description: '当用户添加应用时触发',
    status: 'enabled',
    subscribeUrl: 'https://your-domain.com/events/user-add',
    createdAt: '2024-01-15',
  },
  {
    id: 'event-002',
    name: '用户移除应用事件',
    type: 'app_user_remove',
    description: '当用户移除应用时触发',
    status: 'enabled',
    subscribeUrl: 'https://your-domain.com/events/user-remove',
    createdAt: '2024-01-16',
  },
  {
    id: 'event-003',
    name: '消息接收事件',
    type: 'message_receive',
    description: '当接收到用户消息时触发',
    status: 'enabled',
    subscribeUrl: 'https://your-domain.com/events/message',
    createdAt: '2024-01-20',
  },
  {
    id: 'event-004',
    name: '审批状态变更事件',
    type: 'approval_status_change',
    description: '当审批实例状态变更时触发',
    status: 'disabled',
    subscribeUrl: 'https://your-domain.com/events/approval',
    createdAt: '2024-02-01',
  },
  {
    id: 'event-005',
    name: '日历事件创建',
    type: 'calendar_create',
    description: '当创建新的日历事件时触发',
    status: 'disabled',
    subscribeUrl: 'https://your-domain.com/events/calendar',
    createdAt: '2024-02-10',
  },
  {
    id: 'event-006',
    name: '文档更新事件',
    type: 'drive_update',
    description: '当云文档被更新时触发',
    status: 'disabled',
    subscribeUrl: 'https://your-domain.com/events/drive',
    createdAt: '2024-02-15',
  },
  {
    id: 'event-007',
    name: '群聊添加机器人事件',
    type: 'bot_add_chat',
    description: '当机器人被添加到群聊时触发',
    status: 'enabled',
    subscribeUrl: 'https://your-domain.com/events/bot-chat',
    createdAt: '2024-03-01',
  },
]

// 事件类型选项
export const eventTypes = [
  { value: 'app_user_add', label: '用户添加应用' },
  { value: 'app_user_remove', label: '用户移除应用' },
  { value: 'message_receive', label: '消息接收' },
  { value: 'approval_status_change', label: '审批状态变更' },
  { value: 'calendar_create', label: '日历事件创建' },
  { value: 'drive_update', label: '文档更新' },
  { value: 'bot_add_chat', label: '群聊添加机器人' },
]

export default { eventList, eventTypes }
