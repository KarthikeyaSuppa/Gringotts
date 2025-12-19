import axios from 'axios';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8050';

const api = axios.create({
  baseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT token from localStorage to every request if present
api.interceptors.request.use((config) => {
  try {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers = config.headers || {};
      config.headers['Authorization'] = `Bearer ${token}`;
      // Visible log to indicate token attached (do NOT log token value itself)
      console.log('[api] Attaching Authorization header to', config.method?.toUpperCase(), config.url);
    } else {
      console.log('[api] No token found in localStorage for request', config.method?.toUpperCase(), config.url);
    }
  } catch (e) {
    // ignore
  }
  return config;
}, (error) => Promise.reject(error));

// Response interceptor: handle auth errors globally and log response body when available
api.interceptors.response.use(
  (resp) => resp,
  (error) => {
    const status = error?.response?.status;
    const respData = error?.response?.data;
    console.error('[api] response error', status, respData || error?.message);
    if (status === 401 || status === 403) {
      // Clear client state and redirect to login page
      try {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        localStorage.removeItem('needsProfile');
      } catch (e) {}
      // Use location replacement to avoid keeping bad state in history
      window.location.replace('/');
      return Promise.reject(error);
    }
    return Promise.reject(error);
  }
);

export default api;
