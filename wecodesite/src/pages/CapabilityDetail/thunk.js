const mockCapabilityDetails = {
  '1': {
    id: '1',
    name: '日历管理',
    codeName: 'calendar',
    description: '提供日历事件管理能力，支持创建、查询、修改和删除日历事件',
    status: '启用',
    apiList: [
      { id: 1, name: '创建日历事件', codeName: 'calendar.event.create', method: 'POST' },
      { id: 2, name: '查询日历事件', codeName: 'calendar.event.get', method: 'GET' },
      { id: 3, name: '更新日历事件', codeName: 'calendar.event.update', method: 'PUT' },
      { id: 4, name: '删除日历事件', codeName: 'calendar.event.delete', method: 'DELETE' },
    ],
  },
  '2': {
    id: '2',
    name: '消息推送',
    codeName: 'message',
    description: '提供消息推送能力，支持发送单聊和群聊消息',
    status: '启用',
    apiList: [
      { id: 1, name: '发送消息', codeName: 'message.send', method: 'POST' },
      { id: 2, name: '发送群消息', codeName: 'message.group.send', method: 'POST' },
    ],
  },
  '3': {
    id: '3',
    name: '通讯录',
    codeName: 'contact',
    description: '提供通讯录管理能力',
    status: '启用',
    apiList: [
      { id: 1, name: '获取通讯录', codeName: 'contact.list', method: 'GET' },
      { id: 2, name: '更新通讯录', codeName: 'contact.update', method: 'PUT' },
    ],
  },
  '4': {
    id: '4',
    name: '云空间',
    codeName: 'cloud',
    description: '提供云盘存储能力',
    status: '停用',
    apiList: [
      { id: 1, name: '上传文件', codeName: 'cloud.file.upload', method: 'POST' },
      { id: 2, name: '获取文件列表', codeName: 'cloud.file.list', method: 'GET' },
    ],
  },
  '5': {
    id: '5',
    name: '会议管理',
    codeName: 'meeting',
    description: '提供会议管理能力',
    status: '启用',
    apiList: [
      { id: 1, name: '创建会议', codeName: 'meeting.create', method: 'POST' },
      { id: 2, name: '获取会议列表', codeName: 'meeting.list', method: 'GET' },
      { id: 3, name: '取消会议', codeName: 'meeting.cancel', method: 'POST' },
    ],
  },
};

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const fetchCapabilityDetail = async (capabilityId) => {
  await delay(300);
  return mockCapabilityDetails[capabilityId] || null;
};