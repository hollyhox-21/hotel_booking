import { defineConfig } from "vite";
export default defineConfig({
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: process.env.GATEWAY_URL || "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: process.env.GATEWAY_URL || "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
