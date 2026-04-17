import type { Capability } from '../../types';

export const capabilityTypes: Capability[] = [
  { type: 'bot', name: '机器人', icon: '🤖', enabled: false },
  { type: 'web', name: '网页应用', icon: '🌐', enabled: false },
  { type: 'miniapp', name: '小程序', icon: '📧', enabled: false },
  { type: 'widget', name: '小组件', icon: '📱', enabled: false },
];
