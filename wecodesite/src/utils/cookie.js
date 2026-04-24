const COOKIE_USER_ID_KEY = 'user_id';

export function setUserIdCookie(userId) {
  document.cookie = `${COOKIE_USER_ID_KEY}=${userId}; path=/; max-age=${7 * 24 * 60 * 60}`;
}

export function getUserIdCookie() {
  const cookies = document.cookie.split(';');
  for (const cookie of cookies) {
    const [key, value] = cookie.trim().split('=');
    if (key === COOKIE_USER_ID_KEY) {
      return value;
    }
  }
  return null;
}

export function removeUserIdCookie() {
  document.cookie = `${COOKIE_USER_ID_KEY}=; path=/; max-age=0`;
}

export function isLoggedIn() {
  return !!getUserIdCookie();
}