import type { App } from '../../types';

export const mockApps: App[] = [
  {
    id: '1',
    name: '智能客服机器人',
    icon: '🤖',
    owner: 'zhangsan@company.com',
    role: '管理员',
    updateTime: '2024-01-15 10:30',
    eamap: '企业服务平台',
  },
  {
    id: '2',
    name: '文档协作系统',
    icon: '📄',
    owner: 'lisi@company.com',
    role: '开发者',
    updateTime: '2024-01-14 16:45',
    eamap: null,
  },
  {
    id: '3',
    name: '会议管理系统',
    icon: '📅',
    owner: 'wangwu@company.com',
    role: '开发者',
    updateTime: '2024-01-13 09:20',
    eamap: '办公自动化平台',
  },
  {
    id: '4',
    name: '数据报表平台',
    icon: '📊',
    owner: 'zhaoliu@company.com',
    role: '管理员',
    updateTime: '2024-01-12 14:00',
    eamap: null,
  },
];

export const defaultIcons = ['🤖', '📄', '📅', '📊', '💬', '📧', '📱', '💻', '🌐', '📁', '🔔', '⚙️'];

export const eamapOptions = [
  { value: '1', label: '企业服务平台' },
  { value: '2', label: '办公自动化平台' },
  { value: '3', label: '人力资源系统' },
  { value: '4', label: '财务管理系统' },
];
