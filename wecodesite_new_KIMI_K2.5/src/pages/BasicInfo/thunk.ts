import { mockBasicInfo } from './mock';

export const fetchAppById = async (appId: string) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ ...mockBasicInfo, appId });
    }, 500);
  });
};

export const updateBasicInfo = async (data: typeof mockBasicInfo) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(data);
    }, 500);
  });
};

export const updateAuthType = async (appId: string, authType: string) => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ appId, authType });
    }, 300);
  });
};
