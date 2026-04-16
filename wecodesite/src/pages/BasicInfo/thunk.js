import { mockAppInfo } from './mock';
import { mockApps } from '../AppList/mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const fetchAppInfo = async (appId) => {
  await delay(300);
  return mockAppInfo[appId] || null;
};

export const bindEamapToApp = async (appId, eamap) => {
  await delay(300);
  if (mockAppInfo[appId]) {
    mockAppInfo[appId].eamap = eamap;
  }
  const appInList = mockApps.find((a) => a.id === appId);
  if (appInList) {
    appInList.eamap = eamap;
  }
  return { appId, eamap };
};