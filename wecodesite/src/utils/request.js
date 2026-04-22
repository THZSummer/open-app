import { API_BASE_URL } from './constants';

async function request(url, options = {}) {
  const fullUrl = `${API_BASE_URL}${url}`;
  const response = await fetch(fullUrl, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
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