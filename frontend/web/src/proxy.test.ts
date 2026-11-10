import { afterEach, expect, it, vi } from "vitest";
import viteConfig from "../vite.config";

afterEach(() => vi.unstubAllEnvs());

async function proxyConfig() {
  const config = await (typeof viteConfig === "function"
    ? viteConfig({ command: "serve", mode: "test" })
    : viteConfig);
  return config.server?.proxy;
}

it("preserves the browser host for OAuth callbacks in local development", async () => {
  vi.stubEnv("BACKEND_PROXY_URL", undefined);
  const proxy = { target: "http://localhost:8080", changeOrigin: false };
  expect(await proxyConfig()).toEqual({
    "/api": proxy,
    "/oauth2": proxy,
    "/login": proxy,
  });
});

it("routes container traffic to the backend without using its host in callbacks", async () => {
  vi.stubEnv("BACKEND_PROXY_URL", "http://backend:8080");
  const proxy = { target: "http://backend:8080", changeOrigin: false };
  expect(await proxyConfig()).toEqual({
    "/api": proxy,
    "/oauth2": proxy,
    "/login": proxy,
  });
});
