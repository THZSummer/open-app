import { API_BASE_URL } from './constants';
import { getUserIdCookie } from './cookie';

async function request(url, options = {}) {
  const fullUrl = `${API_BASE_URL}${url}`;
  const userId = getUserIdCookie();
  const headers = {
    'Content-Type': 'application/json',
    ...(userId && { 'X-User-Id': userId }),
    ...options.headers,
  };
  if (userId) {
    headers['Cookie'] = `user_id=${userId}`;
  }
  const response = await fetch(fullUrl, {
    ...options,
    headers,
    credentials: 'include',
  });

  return response.json();
}

export const get = (url, params) => {
  const queryString = new URLSearchParams(params).toString();
  const fullUrl = queryString ? `${url}?${queryString}` : url;
  return request(fullUrl, { method: 'GET' });
};

export const post = (url, data) => request(url, { method: 'POST', body: JSON.stringify(data) });

export const put = (url, data) => request(url, { method: 'PUT', body: JSON.stringify(data) });

export const del = (url) => request(url, { method: 'DELETE' });