import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig(({ mode }) => {
  // 第三個參數 '' 代表讀取所有變數（不限 VITE_ 前綴），可寫在 .env.local 或 shell 環境變數
  const env = loadEnv(mode, process.cwd(), '')

  // 只有「透過 Nginx 以網域存取」時才設 HMR_HOST，例如 HMR_HOST=gta-ch.edwardforce.tw。
  // 本機用 localhost 開發時不要設，否則 HMR WebSocket 會連去外部網域失敗，
  // Vite 客戶端就會不斷重連並整頁重新載入（畫面一直跳動）。
  const hmr = env.HMR_HOST
    ? {
        host: env.HMR_HOST,
        clientPort: Number(env.HMR_CLIENT_PORT) || 443, // 外層 Nginx 用 https (443) 進來
        path: '/carwash/__vite_hmr'
      }
    : true

  return {
    base: '/carwash/',

    plugins: [vue()],

    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },

    server: {
      host: '0.0.0.0',
      port: 5174,
      allowedHosts: [
        'gta-ch.edwardforce.tw'
      ],
      hmr,

      proxy: {
        '/api': {
          target: 'http://localhost:8081',
          changeOrigin: true
        }
      }
    }
  }
})
