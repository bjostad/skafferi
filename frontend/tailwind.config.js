/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f8f6fb',
          100: '#f0ecf6',
          200: '#ded5ec',
          300: '#c5b4dc', // Crisp lilac accent
          400: '#aa90cb',
          500: '#916fb9', // Vibrant scandi purple
          600: '#7c57a4',
          700: '#68458c',
          800: '#553972',
          900: '#462f5d',
          950: '#2b1b3b',
        },
        slate: {
          50: '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748b',
          600: '#475569',
          650: '#384355',
          700: '#334155',
          750: '#263143',
          800: '#1e293b', // Elevated card surface
          850: '#151e2d',
          900: '#0f172a', // Deep slate surface
          950: '#080d1a', // Crisp dark background
        },
        birch: {
          100: '#f7f5f0',
          200: '#ebe6db',
          300: '#ded4c3',
          400: '#ccbfa9',
          500: '#b4a287',
        },
      },
    },
  },
  plugins: [],
}
