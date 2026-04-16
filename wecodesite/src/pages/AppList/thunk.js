import { mockApps, defaultIcons, eamapOptions } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const fetchAppList = async () => {
  await delay(300);
  return mockApps;
};

export const fetchAppById = async (appId) => {
  await delay(200);
  return mockApps.find((a) => a.id === appId) || null;
};

export const fetchDefaultIcons = async () => {
  await delay(200);
  return defaultIcons;
};

export const fetchEamapOptions = async () => {
  await delay(200);
  return eamapOptions;
};

let appIdCounter = mockApps.length + 1;

export const createApp = async (appData) => {
  await delay(300);
  const newApp = {
    id: String(appIdCounter++),
    name: appData.chineseName,
    icon: appData.icon,
    status: '开发中',
    eamap: appData.eamap || null,
    owner: 'currentUser@company.com',
    role: '所有者',
    updateTime: new Date().toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }),
    chineseName: appData.chineseName,
    englishName: appData.englishName,
    chineseDesc: appData.chineseDesc,
    englishDesc: appData.englishDesc,
  };
  mockApps.push(newApp);
  return newApp;
};

export const bindEamap = async (appId, eamap) => {
  await delay(300);
  const app = mockApps.find((a) => a.id === appId);
  if (app) {
    app.eamap = eamap;
  }
  return app;
};