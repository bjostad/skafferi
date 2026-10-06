<template>
  <Transition
    enter-active-class="transition duration-300 ease-out"
    enter-from-class="opacity-0 translate-y-6 scale-95"
    enter-to-class="opacity-100 translate-y-0 scale-100"
    leave-active-class="transition duration-300 ease-in"
    leave-from-class="opacity-100 translate-y-0 scale-100"
    leave-to-class="opacity-0 translate-y-6 scale-95"
  >
    <div
      v-if="toast.show"
      class="fixed bottom-6 inset-x-0 mx-auto w-fit max-w-[90vw] z-50 flex items-center gap-3 px-5 py-3 rounded-2xl shadow-2xl backdrop-blur-md border pointer-events-auto"
      :class="bannerStyles"
      role="status"
      aria-live="polite"
    >
      <!-- Icon -->
      <component :is="iconComponent" class="w-5 h-5 flex-shrink-0" :class="iconColor" />

      <!-- Message -->
      <span class="text-sm font-medium tracking-wide">
        {{ toast.message }}
      </span>

      <!-- Close button -->
      <button
        @click="hideToast"
        type="button"
        class="ml-2 p-1 rounded-full opacity-60 hover:opacity-100 hover:bg-white/10 transition-colors focus:outline-none"
        title="Dismiss"
      >
        <X class="w-4 h-4" />
      </button>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue';
import { useToast } from '../composables/useToast';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-vue-next';

const { toast, hideToast } = useToast();

const bannerStyles = computed(() => {
  switch (toast.value.type) {
    case 'error':
      return 'bg-red-950/90 text-red-200 border-red-500/40 shadow-red-950/50';
    case 'info':
      return 'bg-slate-800/90 text-brand-200 border-brand-400/30 shadow-slate-950/50';
    case 'success':
    default:
      return 'bg-slate-800/95 text-slate-100 border-brand-300/40 shadow-slate-950/60';
  }
});

const iconColor = computed(() => {
  switch (toast.value.type) {
    case 'error':
      return 'text-red-400';
    case 'info':
      return 'text-brand-300';
    case 'success':
    default:
      return 'text-emerald-400';
  }
});

const iconComponent = computed(() => {
  switch (toast.value.type) {
    case 'error':
      return AlertCircle;
    case 'info':
      return Info;
    case 'success':
    default:
      return CheckCircle2;
  }
});
</script>
