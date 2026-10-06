import { reactive } from 'vue';
import api from './api';

const state = reactive({
  version: '',
});

export const appInfo = {
  state,

  async loadVersion() {
    if (state.version) return state.version;
    try {
      const res = await api.getVersion();
      if (res.data && res.data.version) {
        state.version = res.data.version;
      }
    } catch (err) {
      console.warn('Could not load version from backend:', err);
    }
    return state.version;
  },

  getVersionDisplay() {
    if (!state.version) return '';
    return state.version.startsWith('v') ? state.version : `v${state.version}`;
  }
};
