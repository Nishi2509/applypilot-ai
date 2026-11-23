"use client";

import { useEffect, useState, type FormEvent } from "react";
import { api, ApiError } from "@/lib/api";
import { getSession } from "@/lib/session";

type Template = { id: string | null; subject: string; body: string; updatedAt: string | null };
const initial: Template = {
  id: null,
  subject: "Application for {{position}}",
  body: "Dear Hiring Team,\n\nI am writing to apply for the {{position}} position at {{companyName}}. Please find my resume attached for your consideration.\n\nThank you for your time.\n\nSincerely,",
  updatedAt: null
};

export default function EmailTemplates() {
  const [template, setTemplate] = useState<Template>(initial);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!getSession()) { setLoading(false); return; }
    api<Template>("/email-templates").then(setTemplate).catch(e => setError(e instanceof ApiError ? e.message : "Could not load your email template.")).finally(() => setLoading(false));
  }, []);

  async function save(event: FormEvent) {
    event.preventDefault();
    setSaving(true); setError(""); setMessage("");
    try {
      const saved = await api<Template>("/email-templates", { method: "PUT", body: JSON.stringify({ subject: template.subject, body: template.body }) });
      setTemplate(saved); setMessage("Email template saved. New drafts will use it automatically.");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Could not save the email template.");
    } finally { setSaving(false); }
  }

  if (loading) return <p className="lede">Loading your email template…</p>;
  if (!getSession()) return <><h1>Email templates</h1><p className="lede">Sign in to create your reusable application email.</p></>;
  const previewSubject = template.subject.replaceAll("{{companyName}}", "Acme Technologies").replaceAll("{{position}}", "Java Developer");
  const previewBody = template.body.replaceAll("{{companyName}}", "Acme Technologies").replaceAll("{{position}}", "Java Developer");
  return <>
    <div className="topbar"><div><h1>Email templates</h1><p className="lede">Use one editable email for every company while personalizing each draft automatically.</p></div><span className="status pending">Approval required</span></div>
    {error && <div className="message error">{error}</div>}{message && <div className="message success">{message}</div>}
    <section className="card card-pad form-card">
      <h2>Application email template</h2>
      <p className="lede">Use <code>{"{{companyName}}"}</code> and <code>{"{{position}}"}</code>. They are replaced when a company draft is created.</p>
      <form className="upload-form" onSubmit={save}>
        <div className="field"><label htmlFor="template-subject">Subject</label><input id="template-subject" required maxLength={998} value={template.subject} onChange={e => setTemplate({ ...template, subject: e.target.value })} /></div>
        <div className="field"><label htmlFor="template-body">Email body</label><textarea id="template-body" required value={template.body} onChange={e => setTemplate({ ...template, body: e.target.value })} /></div>
        <div className="notice">This creates personalized drafts only. Every email still needs your individual approval before it can be sent.</div>
        <div className="form-actions"><button className="button" disabled={saving}>{saving ? "Saving…" : "Save template"}</button></div>
      </form>
    </section>
    <section className="section"><div className="section-head"><h2>Preview</h2></div><div className="card card-pad"><strong>{previewSubject}</strong><p className="lede">To: hiring@acme.example</p><p style={{ whiteSpace: "pre-wrap" }}>{previewBody}</p></div></section>
  </>;
}
