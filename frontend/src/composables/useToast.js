import { ref } from 'vue';

const toast = ref({
  show: false,
  message: '',
  type: 'success', // 'success' | 'error' | 'info'
});

let timer = null;

export function useToast() {
  function showToast(message, type = 'success', duration = 3000) {
    if (timer) {
      clearTimeout(timer);
    }
    toast.value = {
      show: true,
      message,
      type,
    };
    timer = setTimeout(() => {
      toast.value.show = false;
    }, duration);
  }

  function hideToast() {
    if (timer) {
      clearTimeout(timer);
    }
    toast.value.show = false;
  }

  return {
    toast,
    showToast,
    hideToast,
  };
}
