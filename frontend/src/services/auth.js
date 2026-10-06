import { reactive } from 'vue';
import api from './api';

const state = reactive({
  currentUser: null,
  allUsers: [],
  providers: {},
});

export const auth = {
  state,
  
  async loadUsers() {
    try {
      const [uRes, pRes] = await Promise.allSettled([
        api.getUsers(),
        api.getAuthProviders(),
      ]);

      if (uRes.status === 'fulfilled') {
        state.allUsers = uRes.value.data;
      }
      if (pRes.status === 'fulfilled') {
        state.providers = pRes.value.data;
      }
      
      // Check if URL contains SSO return parameter ?auth_user=<id>
      const urlParams = new URLSearchParams(window.location.search);
      const authUserId = urlParams.get('auth_user');
      if (authUserId) {
        const found = state.allUsers.find(u => u.id === authUserId);
        if (found) {
          this.switchUser(found);
          // Clean URL parameter
          window.history.replaceState({}, document.title, window.location.pathname);
          return;
        }
      }

      // Load selected user from localStorage or pick the first user
      const savedUserId = localStorage.getItem('skafferi_active_user_id');
      if (savedUserId) {
        state.currentUser = state.allUsers.find(u => u.id === savedUserId) || state.allUsers[0] || null;
      } else {
        state.currentUser = state.allUsers[0] || null;
      }
    } catch (err) {
      console.error('Failed to load household users / auth providers:', err);
    }
  },

  switchUser(user) {
    state.currentUser = user;
    localStorage.setItem('skafferi_active_user_id', user.id);
  },

  getCurrentUser() {
    return state.currentUser || {
      username: 'admin',
      displayName: 'Family Admin',
      role: 'ADMIN',
      avatarColor: '#8B5CF6',
    };
  }
};
