<template>
  <div class="relative" ref="dropdownRef">
    <!-- Trigger Button -->
    <button 
      type="button"
      @click="toggleOpen"
      class="h-10 px-3.5 rounded-xl border text-xs font-semibold flex items-center gap-2 transition-all shadow-sm select-none"
      :class="triggerClass">
      <component :is="icon" v-if="icon" class="w-3.5 h-3.5 shrink-0 opacity-80" />
      
      <span class="truncate max-w-[130px] sm:max-w-[160px]">{{ buttonText }}</span>

      <!-- Count Badge when 1+ selected -->
      <span 
        v-if="modelValue.length > 0" 
        class="ml-0.5 px-1.5 py-0.2 rounded-full text-[10px] font-bold bg-brand-400 text-slate-950">
        {{ modelValue.length }}
      </span>

      <ChevronDown class="w-3.5 h-3.5 shrink-0 transition-transform text-slate-400" :class="{ 'rotate-180': isOpen }" />
    </button>

    <!-- Dropdown Menu Popover -->
    <div 
      v-if="isOpen" 
      class="absolute left-0 mt-2 min-w-[220px] max-w-xs bg-slate-900/95 backdrop-blur-md border border-slate-700/90 rounded-2xl shadow-2xl p-2 z-40 ring-1 ring-white/10 space-y-1">
      
      <!-- Header with Select All / Clear -->
      <div class="flex items-center justify-between px-2.5 py-1.5 border-b border-slate-800 text-[11px] font-medium text-slate-400">
        <span>{{ label }}</span>
        <button 
          type="button" 
          @click="toggleAll" 
          class="text-brand-400 hover:text-brand-300 transition-colors font-semibold">
          {{ modelValue.length === options.length ? 'Clear' : 'Select All' }}
        </button>
      </div>

      <!-- Options List -->
      <div class="max-h-60 overflow-y-auto space-y-0.5 py-1 scrollbar-thin">
        <label 
          v-for="opt in options" 
          :key="opt.id"
          class="flex items-center gap-2.5 px-2.5 py-2 rounded-xl text-xs text-slate-200 hover:bg-slate-800/80 cursor-pointer select-none transition-colors group">
          
          <input 
            type="checkbox"
            :checked="modelValue.includes(opt.id)"
            @change="toggleOption(opt.id)"
            class="rounded border-slate-700 text-brand-500 focus:ring-0 focus:ring-offset-0 bg-slate-950 w-3.5 h-3.5 accent-brand-400 cursor-pointer" />

          <!-- Option Color Dot or Icon -->
          <span 
            v-if="opt.color" 
            class="w-2 h-2 rounded-full shrink-0" 
            :style="{ backgroundColor: opt.color }">
          </span>
          <component :is="opt.icon" v-else-if="opt.icon" class="w-3.5 h-3.5 shrink-0 text-slate-400 group-hover:text-white" />

          <span class="flex-1 truncate" :class="{ 'font-semibold text-white': modelValue.includes(opt.id) }">
            {{ opt.name }}
          </span>

          <span v-if="opt.badge" class="text-[10px] text-slate-400 font-mono">
            {{ opt.badge }}
          </span>
        </label>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { ChevronDown } from 'lucide-vue-next';

const props = defineProps({
  label: {
    type: String,
    required: true,
  },
  icon: {
    type: Object,
    default: null,
  },
  options: {
    type: Array,
    required: true,
  },
  modelValue: {
    type: Array,
    default: () => [],
  },
  allLabel: {
    type: String,
    default: 'All',
  },
});

const emit = defineEmits(['update:modelValue', 'change']);

const isOpen = ref(false);
const dropdownRef = ref(null);

function toggleOpen() {
  isOpen.value = !isOpen.value;
}

function close() {
  isOpen.value = false;
}

function handleClickOutside(event) {
  if (dropdownRef.value && !dropdownRef.value.contains(event.target)) {
    close();
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside);
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside);
});

const buttonText = computed(() => {
  if (!props.modelValue || props.modelValue.length === 0) {
    return `${props.label}: ${props.allLabel}`;
  }
  if (props.modelValue.length === 1) {
    const match = props.options.find(o => o.id === props.modelValue[0]);
    return match ? match.name : `${props.label}: 1`;
  }
  return props.label;
});

const triggerClass = computed(() => {
  const hasSelection = props.modelValue && props.modelValue.length > 0;
  if (isOpen.value) {
    return 'bg-slate-800 text-white border-brand-400 ring-1 ring-brand-400/30';
  }
  if (hasSelection) {
    return 'bg-brand-500/15 text-brand-200 border-brand-400/60 shadow-sm';
  }
  return 'bg-slate-900 text-slate-300 hover:text-white border-slate-700 hover:border-slate-600';
});

function toggleOption(id) {
  const current = [...props.modelValue];
  const idx = current.indexOf(id);
  if (idx > -1) {
    current.splice(idx, 1);
  } else {
    current.push(id);
  }
  emit('update:modelValue', current);
  emit('change', current);
}

function toggleAll() {
  if (props.modelValue.length === props.options.length) {
    emit('update:modelValue', []);
    emit('change', []);
  } else {
    const allIds = props.options.map(o => o.id);
    emit('update:modelValue', allIds);
    emit('change', allIds);
  }
}
</script>
