import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.VITE_API_PROXY_TARGET || env.VITE_API_BASE_URL || 'http://localhost:8081'

  return {
    plugins: [vue()],
    server: {
      proxy: {
        '/user': target,
        '/shop': target,
        '/shop-type': target,
        '/blog': target,
        '/follow': target,
        '/voucher': target,
        '/voucher-order': target,
        '/upload': target,
      },
    },
  }
})
