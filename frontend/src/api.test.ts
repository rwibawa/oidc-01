import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchCurrentUser, fetchCsrfToken, logout } from "./api";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("authentication API", () => {
  it("treats an unauthenticated session as signed out", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 401 })));

    await expect(fetchCurrentUser()).resolves.toBeNull();
  });

  it("returns the current user from the protected endpoint", async () => {
    const user = {
      subject: "google-sub-123",
      name: "Taylor Example",
      email: "taylor@example.com",
    };
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(Response.json(user)),
    );

    await expect(fetchCurrentUser()).resolves.toEqual(user);
  });

  it("surfaces API failures instead of reporting a signed-out state", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 500 })));

    await expect(fetchCurrentUser()).rejects.toThrow("Loading account failed (HTTP 500).");
  });

  it("sends the CSRF token when signing out", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);
    const token = { headerName: "X-XSRF-TOKEN", token: "csrf-value" };

    await logout(token);

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/logout",
      expect.objectContaining({
        method: "POST",
        headers: { "X-XSRF-TOKEN": "csrf-value" },
      }),
    );
  });

  it("loads the CSRF token returned by the backend", async () => {
    const token = { headerName: "X-XSRF-TOKEN", token: "csrf-value" };
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(Response.json(token)));

    await expect(fetchCsrfToken()).resolves.toEqual(token);
  });
});
