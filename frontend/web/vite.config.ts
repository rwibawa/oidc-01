import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { loadEnv } from "vite";

export default defineConfig(({ mode }) => {
  const backendUrl = loadEnv(mode, ".", "").BACKEND_PROXY_URL ?? "http://localhost:8080";
  const backendProxy = { target: backendUrl, changeOrigin: false };

  return {
    plugins: [react()],
    server: {
      proxy: {
        "/api": backendProxy,
        "/oauth2": backendProxy,
        "/login": backendProxy,
      },
    },
    test: {
      environment: "node",
    },
  };
});
