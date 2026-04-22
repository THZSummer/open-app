import { mockVersions } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const fetchVersionList = async () => {
  await delay(300);
  return mockVersions;
};