import { mockApps, defaultIcons, eamapOptions } from './mock';
import type { App } from '../../types';

export const fetchAppList = async (): Promise<App[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockApps);
    }, 500);
  });
};

export const fetchDefaultIcons = async (): Promise<string[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(defaultIcons);
    }, 300);
  });
};

export const fetchEamapOptions = async (): Promise<{ value: string; label: string }[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(eamapOptions);
    }, 300);
  });
};

export const createApp = async (values: {
  icon: string;
  nameZh: string;
  nameEn: string;
  descZh: string;
  descEn: string;
  eamap?: string;
}): Promise<App> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const newApp: App = {
        id: String(Date.now()),
        name: values.nameZh,
        icon: values.icon,
        owner: 'current@user.com',
        role: '管理员',
        updateTime: new Date().toLocaleString('zh-CN'),
        eamap: values.eamap ? eamapOptions.find(o => o.value === values.eamap)?.label || null : null,
      };
      resolve(newApp);
    }, 500);
  });
};
