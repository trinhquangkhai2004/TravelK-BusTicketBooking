import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '^/booking/': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/trips': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/auth': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/user': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/payment': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/admin': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      // Thêm proxy cho Chat API
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
