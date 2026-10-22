import { fileURLToPath, URL } from 'node:url'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/ · https://vitest.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    rolldownOptions: {
      output: {
        // Stable vendor chunks: browsers keep them cached across deploys that only touch app code.
        codeSplitting: {
          groups: [
            {
              name: 'react',
              test: /[\\/]node_modules[\\/](react|react-dom|scheduler|react-router)[\\/]/,
            },
            { name: 'mui', test: /[\\/]node_modules[\\/](@mui|@emotion)[\\/]/ },
            { name: 'vendor', test: /[\\/]node_modules[\\/]/ },
          ],
        },
      },
    },
  },
  // The API's CORS allowlist only accepts http://localhost:5173, so the port must not drift.
  server: {
    host: 'localhost',
    port: 5173,
    strictPort: true,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts'],
    globals: true,
    css: false,
    clearMocks: true,
    // Deterministic across machines: a fixed API origin for MSW and the app's time zone for date formatting.
    env: { VITE_API_URL: 'http://api.test/api/v1', TZ: 'America/Sao_Paulo' },
    // jsdom enforces CORS like a browser; serving the test page from the API origin keeps requests
    // same-origin, so MSW handlers need no CORS headers (the real API's CORS is tested in the backend).
    environmentOptions: { jsdom: { url: 'http://api.test/' } },
  },
})
