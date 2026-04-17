import { mockVersions } from './mock';
import type { Version } from '../../types';

export const fetchVersions = async (): Promise<Version[]> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockVersions);
    }, 500);
  });
};

export const createVersion = async (data: {
  version: string;
  description: string;
}): Promise<Version> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const newVersion: Version = {
        id: Date.now(),
        version: data.version,
        status: '审核中',
        publisher: '当前用户',
        approvedTime: '-',
      };
      resolve(newVersion);
    }, 300);
  });
};

export const deleteVersion = async (_id: number): Promise<void> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve();
    }, 200);
  });
};
