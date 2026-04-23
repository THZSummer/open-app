export const API_CONFIG = {
  BASE_URL: '/api/v1',

  APIS: {
    LIST: '/apis',
    DETAIL: '/apis/{id}',
    CREATE: '/apis',
    UPDATE: '/apis/{id}',
    DELETE: '/apis/{id}',
    WITHDRAW: '/apis/{id}/withdraw',
    CATEGORIES: '/categories',
  },

  EVENTS: {
    LIST: '/events',
    DETAIL: '/events/{id}',
    CREATE: '/events',
    UPDATE: '/events/{id}',
    DELETE: '/events/{id}',
    WITHDRAW: '/events/{id}/withdraw',
  },

  CALLBACKS: {
    LIST: '/callbacks',
    DETAIL: '/callbacks/{id}',
    CREATE: '/callbacks',
    UPDATE: '/callbacks/{id}',
    DELETE: '/callbacks/{id}',
    WITHDRAW: '/callbacks/{id}/withdraw',
  },

  APP_APIS: {
    LIST: '/apps/{appId}/apis',
    SUBSCRIBE: '/apps/{appId}/apis/subscribe',
    WITHDRAW: '/apps/{appId}/apis/{id}/withdraw',
  },

  APP_EVENTS: {
    LIST: '/apps/{appId}/events',
    SUBSCRIBE: '/apps/{appId}/events/subscribe',
    CONFIG: '/apps/{appId}/events/{id}/config',
    WITHDRAW: '/apps/{appId}/events/{id}/withdraw',
  },

  APP_CALLBACKS: {
    LIST: '/apps/{appId}/callbacks',
    SUBSCRIBE: '/apps/{appId}/callbacks/subscribe',
    CONFIG: '/apps/{appId}/callbacks/{id}/config',
    WITHDRAW: '/apps/{appId}/callbacks/{id}/withdraw',
  },

  CATEGORIES: {
    LIST: '/categories',
    DETAIL: '/categories/{id}',
    CREATE: '/categories',
    UPDATE: '/categories/{id}',
    DELETE: '/categories/{id}',
    OWNERS: '/categories/{id}/owners',
    APIS: '/categories/{id}/apis',
    EVENTS: '/categories/{id}/events',
    CALLBACKS: '/categories/{id}/callbacks',
  },

  APPROVALS: {
    PENDING: '/approvals/pending',
    DETAIL: '/approvals/{id}',
    APPROVE: '/approvals/{id}/approve',
    REJECT: '/approvals/{id}/reject',
    CANCEL: '/approvals/{id}/cancel',
    BATCH_APPROVE: '/approvals/batch-approve',
    BATCH_REJECT: '/approvals/batch-reject',
  },

  APPROVAL_FLOWS: {
    LIST: '/approval-flows',
    DETAIL: '/approval-flows/{id}',
    CREATE: '/approval-flows',
    UPDATE: '/approval-flows/{id}',
  },
};

export const buildApiUrl = (template, params = {}) => {
  let url = template;
  Object.keys(params).forEach(key => {
    url = url.replace(`{${key}}`, params[key]);
  });
  return url;
};

export const fetchApi = async (url, options = {}) => {
  const { params, ...fetchOptions } = options;
  let fullUrl = `${API_CONFIG.BASE_URL}${url}`;
  if (params) {
    const queryString = new URLSearchParams(params).toString();
    fullUrl = queryString ? `${fullUrl}?${queryString}` : fullUrl;
  }
  const response = await fetch(fullUrl, {
    ...fetchOptions,
    headers: {
      'Content-Type': 'application/json',
      ...fetchOptions.headers,
    },
  });
  return response.json();
};