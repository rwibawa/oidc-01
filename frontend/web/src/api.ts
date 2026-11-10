export interface CurrentUser {
  subject: string;
  name: string;
  email: string | null;
}

export interface CsrfToken {
  headerName: string;
  token: string;
}

async function requireOk(response: Response, action: string): Promise<void> {
  if (!response.ok) {
    throw new Error(`${action} failed (HTTP ${response.status}).`);
  }
}

export async function fetchCsrfToken(): Promise<CsrfToken> {
  const response = await fetch("/api/csrf", { credentials: "include" });
  await requireOk(response, "Loading security token");
  const value: unknown = await response.json();

  if (
    typeof value !== "object" ||
    value === null ||
    !("headerName" in value) ||
    typeof value.headerName !== "string" ||
    !("token" in value) ||
    typeof value.token !== "string"
  ) {
    throw new Error("The server returned an invalid security token.");
  }

  return { headerName: value.headerName, token: value.token };
}

export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  const response = await fetch("/api/me", { credentials: "include" });
  if (response.status === 401) {
    return null;
  }
  await requireOk(response, "Loading account");
  const value: unknown = await response.json();

  if (
    typeof value !== "object" ||
    value === null ||
    !("subject" in value) ||
    typeof value.subject !== "string" ||
    !("name" in value) ||
    typeof value.name !== "string" ||
    !("email" in value) ||
    (typeof value.email !== "string" && value.email !== null)
  ) {
    throw new Error("The server returned an invalid account.");
  }

  return {
    subject: value.subject,
    name: value.name,
    email: value.email,
  };
}

export async function logout(csrfToken: CsrfToken): Promise<void> {
  const response = await fetch("/api/logout", {
    method: "POST",
    credentials: "include",
    headers: { [csrfToken.headerName]: csrfToken.token },
  });
  await requireOk(response, "Signing out");
}
