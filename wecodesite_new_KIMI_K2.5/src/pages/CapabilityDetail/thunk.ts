import { capabilityConfig } from './mock';

export const fetchCapabilityConfig = async (type: string): Promise<Record<string, string>> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(capabilityConfig[type] || {});
    }, 300);
  });
};

export const saveCapabilityConfig = async (_type: string, _config: Record<string, string>): Promise<void> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve();
    }, 300);
  });
};
