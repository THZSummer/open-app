import { mockApis, availableApis } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const fetchApiList = async () => {
  await delay(300);
  return mockApis;
};

export const fetchAvailableApis = async (auth) => {
  await delay(300);
  return availableApis[auth] || null;
};

export const fetchApiModules = async (auth) => {
  await delay(300);
  return availableApis[auth]?.modules || [];
};