import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Локально: VITE_API_BASE_URL не задан — axios шлёт на тот же origin, Vite проксирует /api на бэкенд.
// Render Static Site: задайте VITE_API_BASE_URL=https://<ваш-spring-сервис>.onrender.com в Environment.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
