import { reactive } from 'vue';
import api from './api';

const DEFAULT_APP_NAME = 'Skafferi';

const getInitialAppName = () => {
  if (typeof window !== 'undefined' && window.localStorage) {
    try {
      const saved = localStorage.getItem('skafferi_app_name');
      if (saved && saved.trim()) return saved.trim();
    } catch (e) {
      // ignore
    }
  }
  return DEFAULT_APP_NAME;
};

const state = reactive({
  version: '',
  appName: getInitialAppName(),
});

export const appInfo = {
  state,

  async loadVersion() {
    try {
      const res = await api.getVersion();
      if (res.data) {
        if (res.data.version) {
          state.version = res.data.version;
        }
        if (res.data.appName !== undefined) {
          this.setAppName(res.data.appName);
        }
      }
    } catch (err) {
      console.warn('Could not load version/app info from backend:', err);
    }
    return state.version;
  },

  getAppName() {
    return (state.appName && state.appName.trim()) ? state.appName.trim() : DEFAULT_APP_NAME;
  },

  setAppName(name) {
    const resolved = (name && typeof name === 'string' && name.trim()) ? name.trim() : DEFAULT_APP_NAME;
    state.appName = resolved;
    if (typeof window !== 'undefined' && window.localStorage) {
      try {
        localStorage.setItem('skafferi_app_name', resolved);
      } catch (err) {
        // ignore
      }
    }
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('app-name-changed', { detail: { appName: resolved } }));
    }
  },

  getVersionDisplay() {
    if (!state.version) return '';
    return state.version.startsWith('v') ? state.version : `v${state.version}`;
  }
};
