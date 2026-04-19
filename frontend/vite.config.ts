import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 3000,
    host: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // ⭐ [수정 3] 환율 API CORS 우회 프록시
      '/exchange-proxy': {
        target: 'https://open.er-api.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/exchange-proxy/, '')
      }
    }
  }
})