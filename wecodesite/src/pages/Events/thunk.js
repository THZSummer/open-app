import { mockEvents, mockSubscriptionConfig, mockAllEvents } from './mock';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const fetchEventList = async () => {
  await delay(300);
  return mockEvents;
};

export const fetchSubscriptionConfig = async () => {
  await delay(300);
  return mockSubscriptionConfig;
};

export const fetchAllEvents = async () => {
  await delay(300);
  return mockAllEvents;
};