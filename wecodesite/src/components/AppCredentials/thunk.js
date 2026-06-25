import { API_CONFIG, fetchApi } from '../../configs/web.config';

export const fetchAppIdentity = async (appId) => {
  try {
    const result = await fetchApi(API_CONFIG.APP.IDENTITY, { params: { appId } });
    return result || {};
  } catch (err) {
    return {};
  }
};
