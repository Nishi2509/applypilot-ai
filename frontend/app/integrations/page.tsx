"use client";

import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { getSession } from "@/lib/session";

type Connection = { configured: boolean; connected: boolean; detail: string };
type Status = { gmail: Connection; googleSheets: Connection; aiProvider: Connection };
type OAuthStart = { authorizationUrl: string };

export default function Integrations() {
  const [status, setStatus] = useState<Status | null>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const load = () => {
    if (getSession()) api<Status>("/integrations/status").then(setStatus).catch(e => setError(e instanceof ApiError ? e.message : "Unable to load integration status."));
  };
  useEffect(load, []);
  async function connect() {
    setBusy(true); setError("");
    try { const start = await api<OAuthStart>("/integrations/gmail/connect", { method: "POST" }); window.location.assign(start.authorizationUrl); }
    catch (e) { setError(e instanceof ApiError ? e.message : "Unable to start Google connection."); setBusy(false); }
  }
  const row = (name: string, description: string, connection?: Connection, connectable = false) => <>
    <div className="section-head"><div><h2>{name}</h2><p className="lede">{description}</p></div><div style={{ display: "flex", alignItems: "center", gap: 12 }}><span className={`status ${connection?.connected ? "ready" : "pending"}`}>{connection?.connected ? "Connected" : connection?.configured ? "Connection required" : "Not configured"}</span>{connectable && connection?.configured && !connection.connected && <button className="button" onClick={connect} disabled={busy}>{busy ? "Opening Google..." : "Connect Google"}</button>}</div></div>
    <p style={{ color: "#64748b", fontSize: ".82rem", margin: "-4px 0 18px" }}>{connection?.detail ?? "Sign in to view status."}</p>
  </>;
  return <><div className="topbar"><div><h1>Integrations</h1><p className="lede">Connect services only through secure OAuth flows.</p></div></div>{error && <div className="message error">{error}</div>}<div className="card card-pad">{row("Gmail", "Used only after you explicitly approve an individual application.", status?.gmail, true)}<hr style={{ border: 0, borderTop: "1px solid #e4e9f2", margin: "18px 0" }} />{row("Google Sheets", "A reporting layer; MySQL remains the source of truth.", status?.googleSheets, true)}<hr style={{ border: 0, borderTop: "1px solid #e4e9f2", margin: "18px 0" }} />{row("AI provider", "The private AI service provides structured analysis, never email sending.", status?.aiProvider)}</div></>;
}
