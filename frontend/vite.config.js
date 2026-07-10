import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  define: {
    global: 'window'
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8081',
      '/ws': {
        target: 'http://localhost:8081',
        ws: true
      },
      '/uploads': 'http://localhost:8081'
    }
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true
  }
})
