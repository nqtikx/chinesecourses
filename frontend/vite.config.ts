import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/attendance': 'http://localhost:8080',
      '/enrollments': 'http://localhost:8080',
      '/group-schedule-rules': 'http://localhost:8080',
      '/lesson-sessions': 'http://localhost:8080',
      '/persons': 'http://localhost:8080',
      '/study-groups': 'http://localhost:8080',
      '/teachers': 'http://localhost:8080',
    },
  },
})
