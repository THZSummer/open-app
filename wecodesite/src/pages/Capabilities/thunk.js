const mockCapabilities = [
  {
    id: '1',
    name: '日历管理',
    codeName: 'calendar',
    description: '提供日历事件管理能力',
    apiCount: 12,
    status: '启用',
  },
  {
    id: '2',
    name: '消息推送',
    codeName: 'message',
    description: '提供消息推送能力',
    apiCount: 8,
    status: '启用',
  },
  {
    id: '3',
    name: '通讯录',
    codeName: 'contact',
    description: '提供通讯录管理能力',
    apiCount: 6,
    status: '启用',
  },
  {
    id: '4',
    name: '云空间',
    codeName: 'cloud',
    description: '提供云盘存储能力',
    apiCount: 10,
    status: '停用',
  },
  {
    id: '5',
    name: '会议管理',
    codeName: 'meeting',
    description: '提供会议管理能力',
    apiCount: 7,
    status: '启用',
  },
];

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const fetchCapabilityList = async () => {
  await delay(300);
  return mockCapabilities;
};