export type Session = { accessToken: string; userId: string; email: string; displayName: string };
const key = "applypilot.session";
export function getSession(): Session | null { if (typeof window === "undefined") return null; try { return JSON.parse(window.localStorage.getItem(key) ?? "null") as Session | null; } catch { return null; } }
export function saveSession(session: Session) { window.localStorage.setItem(key, JSON.stringify(session)); }
export function clearSession() { window.localStorage.removeItem(key); }
