// Mock 应用能力数据
export const capabilities = [
  {
    id: 'cap-001',
    name: '机器人',
    type: 'bot',
    icon: '🤖',
    description: '创建智能对话机器人，支持自定义回复和交互流程',
    status: 'enabled',
  },
  {
    id: 'cap-002',
    name: '消息推送',
    type: 'message',
    icon: '📨',
    description: '向用户或群组发送文本、卡片、图片等多种类型的消息',
    status: 'enabled',
  },
  {
    id: 'cap-003',
    name: '日历',
    type: 'calendar',
    icon: '📅',
    description: '管理日历事件，创建、修改、删除会议和日程',
    status: 'disabled',
  },
  {
    id: 'cap-004',
    name: '云文档',
    type: 'drive',
    icon: '📄',
    description: '操作云文档和文件夹，支持上传、下载、分享等功能',
    status: 'disabled',
  },
  {
    id: 'cap-005',
    name: '审批',
    type: 'approval',
    icon: '✅',
    description: '创建和管理审批流程，支持自定义审批表单',
    status: 'disabled',
  },
  {
    id: 'cap-006',
    name: '通讯录',
    type: 'contact',
    icon: '👥',
    description: '读取和管理企业通讯录信息',
    status: 'disabled',
  },
  {
    id: 'cap-007',
    name: '视频会议',
    type: 'meeting',
    icon: '📹',
    description: '创建和管理视频会议，支持会议控制和录制',
    status: 'disabled',
  },
  {
    id: 'cap-008',
    name: '邮箱',
    type: 'mail',
    icon: '✉️',
    description: '发送和接收邮件，管理邮箱文件夹',
    status: 'disabled',
  },
]

export default capabilities
