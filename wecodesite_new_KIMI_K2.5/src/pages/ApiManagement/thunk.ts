import { mockApis, mockAvailableApis, mockModules } from './mock';
import type { ApiPermission } from '../../types';

export const fetchApiList = async (): Promise<ApiPermission[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockApis);
    }, 500);
  });
};

export const fetchAvailableApis = async (type: 'soa' | 'apig') => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockAvailableApis[type]);
    }, 300);
  });
};

export const fetchApiModules = async (_type: 'soa' | 'apig') => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockModules);
    }, 200);
  });
};
