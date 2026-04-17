export const capabilityConfig: Record<string, Record<string, string>> = {
  bot: {
    botName: '智能助手',
    botDesc: '您的智能助手，随时为您提供帮助',
  },
  web: {
    homepageUrl: 'https://example.com',
    mobileHomepageUrl: 'https://m.example.com',
    openMode: 'sidebar',
  },
  miniapp: {
    appName: '轻应用',
    appDesc: '轻量级应用，快速响应',
  },
  widget: {
    widgetName: '快捷组件',
    widgetDesc: '便捷的桌面组件',
  },
};

export const capabilityFields: Record<string, { name: string; label: string; required: boolean }[]> = {
  bot: [
    { name: 'botName', label: '机器人名称', required: true },
    { name: 'botDesc', label: '机器人描述', required: false },
  ],
  web: [
    { name: 'homepageUrl', label: '桌面端主页地址', required: true },
    { name: 'mobileHomepageUrl', label: '移动端主页地址', required: false },
    { name: 'openMode', label: '桌面端主页打开方式', required: false },
  ],
  miniapp: [
    { name: 'appName', label: '小程序名称', required: true },
    { name: 'appDesc', label: '小程序描述', required: false },
  ],
  widget: [
    { name: 'widgetName', label: '小组件名称', required: true },
    { name: 'widgetDesc', label: '小组件描述', required: false },
  ],
};
