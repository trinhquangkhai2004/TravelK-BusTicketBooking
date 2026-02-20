import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
        // tách ra booking$ và booking/ để làm hai 2 th cụ thể cho API booking/ ở BE
      '^/booking$': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
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
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/buses': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/station': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
