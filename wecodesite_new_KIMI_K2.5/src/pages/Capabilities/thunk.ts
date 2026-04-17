import { capabilityTypes } from './mock';
import type { Capability } from '../../types';

export const fetchCapabilities = async (enabledTypes: string[]): Promise<Capability[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const caps = capabilityTypes.map(cap => ({
        ...cap,
        enabled: enabledTypes.includes(cap.type),
      }));
      resolve(caps);
    }, 300);
  });
};

export const enableCapability = async (_type: string): Promise<void> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve();
    }, 300);
  });
};
