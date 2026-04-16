export default {
  apis: [
    {
      id: '1',
      name: '获取用户信息',
      codeName: 'user.get',
      authType: 'SOA',
      category: '用户管理',
      status: '已审核'
    },
    {
      id: '2',
      name: '发送消息',
      codeName: 'message.send',
      authType: 'APIG',
      category: '消息推送',
      status: '审核中'
    },
    {
      id: '3',
      name: '创建日历事件',
      codeName: 'calendar.event.create',
      authType: 'SOA',
      category: '日历管理',
      status: '已中止'
    }
  ],
  availableApis: {
    soa: [
      {
        id: '1',
        name: '获取用户详情',
        codeName: 'user.get',
        category: '用户管理',
        needApproval: true
      },
      {
        id: '2',
        name: '获取用户列表',
        codeName: 'user.list',
        category: '用户管理',
        needApproval: false
      },
      {
        id: '3',
        name: '获取部门详情',
        codeName: 'department.get',
        category: '组织架构',
        needApproval: true
      },
      {
        id: '4',
        name: '获取部门列表',
        codeName: 'department.list',
        category: '组织架构',
        needApproval: false
      }
    ],
    apig: [
      {
        id: '5',
        name: '发送文本消息',
        codeName: 'message.send.text',
        category: '消息推送',
        needApproval: true
      },
      {
        id: '6',
        name: '发送图片消息',
        codeName: 'message.send.image',
        category: '消息推送',
        needApproval: true
      },
      {
        id: '7',
        name: '获取文件信息',
        codeName: 'file.get',
        category: '云空间',
        needApproval: false
      },
      {
        id: '8',
        name: '上传文件',
        codeName: 'file.upload',
        category: '云空间',
        needApproval: true
      }
    ]
  }
};