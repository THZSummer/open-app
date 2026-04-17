import { mockEvents, mockAllEvents } from './mock';
import type { Event } from '../../types';

export const fetchEventList = async (page: number, pageSize: number): Promise<{ list: Event[]; total: number }> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      resolve({
        list: mockEvents.slice(start, end),
        total: mockEvents.length,
      });
    }, 300);
  });
};

export const fetchSubscriptionConfig = async (): Promise<{ method: string }> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({ method: 'mqs' });
    }, 200);
  });
};

export const fetchAllEvents = async (page: number, pageSize: number): Promise<{ list: Event[]; total: number }> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      const start = (page - 1) * pageSize;
      const end = start + pageSize;
      resolve({
        list: mockAllEvents.slice(start, end),
        total: mockAllEvents.length,
      });
    }, 300);
  });
};

export const addEvents = async (_eventIds: string[]): Promise<void> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve();
    }, 300);
  });
};

export const removeEvent = async (_eventId: string): Promise<void> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve();
    }, 200);
  });
};
