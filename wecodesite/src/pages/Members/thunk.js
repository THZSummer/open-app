import { mockMembers } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const fetchMemberList = async () => {
  await delay(300);
  return mockMembers;
};