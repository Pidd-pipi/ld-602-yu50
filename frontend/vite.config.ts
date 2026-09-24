import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 20102,
    host: "0.0.0.0",
    proxy: {
      // 与 frontend/nginx.conf 的 location /api/ 反向代理保持一致
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
});
