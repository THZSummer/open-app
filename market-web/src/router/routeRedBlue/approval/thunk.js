import API_CONFIG from '../../../configs/web.config';
import { fetchApi } from '../../../utils/webFetch';

export const fetchPendingList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APPROVAL_PENDING_LIST, { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const fetchPublishedList = async (params = {}) => {
  try {
    const result = await fetchApi(API_CONFIG.APPROVAL_PUBLISHED_LIST, { method: 'GET', params });
    return result || {};
  } catch (err) {
    return {};
  }
};

export const processApproval = async (data) => {
  try {
    const result = await fetchApi(API_CONFIG.APPROVAL_PROCESS, {
      method: 'POST',
      body: JSON.stringify(data),
    });
    return result || {};
  } catch (err) {
    return {};
  }
};
