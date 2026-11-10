import { useEffect, useState } from "react";
import {
  fetchCsrfToken,
  fetchCurrentUser,
  logout,
  type CurrentUser,
  type CsrfToken,
} from "./api";

type ViewState = "loading" | "signed-out" | "signed-in" | "error";

export default function App() {
  const [view, setView] = useState<ViewState>("loading");
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [csrfToken, setCsrfToken] = useState<CsrfToken | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    async function loadSession() {
      try {
        const token = await fetchCsrfToken();
        const currentUser = await fetchCurrentUser();
        if (active) {
          setCsrfToken(token);
          setUser(currentUser);
          setView(currentUser ? "signed-in" : "signed-out");
        }
      } catch (reason) {
        if (active) {
          setError(
            reason instanceof Error ? reason.message : "Unable to load your account.",
          );
          setView("error");
        }
      }
    }

    void loadSession();
    return () => {
      active = false;
    };
  }, []);

  async function handleLogout() {
    if (!csrfToken) {
      return;
    }

    setError(null);
    try {
      await logout(csrfToken);
      setCsrfToken(null);
      setUser(null);
      setView("signed-out");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Unable to sign out.");
    }
  }

  return (
    <main className="page">
      <section className="card" aria-labelledby="page-title">
        <div className="brand-mark" aria-hidden="true">
          O
        </div>
        <p className="eyebrow">YOUR ACCOUNT</p>
        <h1 id="page-title">
          {view === "signed-in" ? "You’re signed in" : "Welcome"}
        </h1>

        {view === "loading" && (
          <p className="muted" role="status">
            Checking your session…
          </p>
        )}

        {view === "signed-out" && (
          <>
            <p className="muted">Sign in securely with your Google account.</p>
            <a className="google-button" href="/oauth2/authorization/google">
              <span className="google-g" aria-hidden="true">
                G
              </span>
              Continue with Google
            </a>
          </>
        )}

        {view === "signed-in" && user && (
          <div className="account">
            <p className="user-name">{user.name}</p>
            {user.email && <p className="muted">{user.email}</p>}
            <p className="subject">
              Account ID <code>{user.subject}</code>
            </p>
            <button className="sign-out-button" onClick={handleLogout}>
              Sign out
            </button>
          </div>
        )}

        {view === "error" && error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}

        {view === "signed-in" && error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
      </section>
      <p className="footer">Authentication secured by OpenID Connect</p>
    </main>
  );
}
