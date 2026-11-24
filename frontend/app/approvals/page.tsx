"use client";

import { useEffect, useState, type FormEvent } from "react";
import { api, ApiError } from "@/lib/api";

type Draft = {
  applicationId: string;
  recipientEmail: string;
  subject: string;
  body: string;
  attachmentMode: "MASTER_PDF" | "TAILORING_REQUIRED";
  attachmentNote: string;
  status: string;
  emailVersion: number;
};
type DraftEdit = Pick<Draft, "recipientEmail" | "subject" | "body">;
type SheetImport = { drafts: Draft[]; skippedRows: string[] };

const initial = { companyName: "", hrEmail: "", position: "", location: "", jobDescription: "" };
const initialSheet = { spreadsheet: "", range: "", location: "" };

export default function Approvals() {
  const [form, setForm] = useState(initial);
  const [sheetForm, setSheetForm] = useState(initialSheet);
  const [drafts, setDrafts] = useState<Draft[]>([]);
  const [saving, setSaving] = useState(false);
  const [importing, setImporting] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editForm, setEditForm] = useState<DraftEdit>({ recipientEmail: "", subject: "", body: "" });
  const [actionInProgress, setActionInProgress] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const load = () => api<Draft[]>("/applications/email-drafts").then(setDrafts).catch(() => setDrafts([]));
  useEffect(() => { void load(); }, []);

  async function create(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(""); setMessage("");
    try {
      const created = await api<Draft>("/applications/manual-email-drafts", { method: "POST", body: JSON.stringify(form) });
      setDrafts(current => [created, ...current]);
      setForm(initial);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Could not create the draft. Upload a master resume first.");
    } finally {
      setSaving(false);
    }
  }

  async function importSheet(event: FormEvent) {
    event.preventDefault();
    setImporting(true); setError(""); setMessage("");
    try {
      const result = await api<SheetImport>("/applications/google-sheet-email-drafts", { method: "POST", body: JSON.stringify(sheetForm) });
      setDrafts(current => [...result.drafts, ...current]);
      setMessage(`${result.drafts.length} personalized draft${result.drafts.length === 1 ? "" : "s"} created from Google Sheets.${result.skippedRows.length ? ` ${result.skippedRows.join(" ")}` : ""}`);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Could not import drafts from Google Sheets.");
    } finally { setImporting(false); }
  }

  function beginEditing(draft: Draft) {
    setError("");
    setEditingId(draft.applicationId);
    setEditForm({ recipientEmail: draft.recipientEmail, subject: draft.subject, body: draft.body });
  }

  function cancelEditing() {
    setEditingId(null);
    setEditForm({ recipientEmail: "", subject: "", body: "" });
  }

  async function saveEdit(event: FormEvent, id: string) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const updated = await api<Draft>(`/applications/${id}/email-draft`, { method: "PATCH", body: JSON.stringify(editForm) });
      setDrafts(current => current.map(draft => draft.applicationId === id ? updated : draft));
      cancelEditing();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Could not save your draft changes.");
    } finally {
      setSaving(false);
    }
  }

  async function action(id: string, actionName: "approve-email" | "send-email") {
    if (actionInProgress) return;
    setActionInProgress(id);
    setError("");
    try {
      const updated = await api<Draft>(`/applications/${id}/${actionName}`, { method: "POST" });
      setDrafts(current => current.map(draft => draft.applicationId === id ? updated : draft));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "The action could not be completed.");
    } finally {
      setActionInProgress(null);
    }
  }

  return <>
    <div className="topbar">
      <div><h1>Approval Center</h1><p className="lede">Create, edit, approve, and explicitly send application emails.</p></div>
      <span className="status pending">No automatic sends</span>
    </div>
    {error && <div className="message error">{error}</div>}{message && <div className="message success">{message}</div>}

    <section className="card card-pad form-card">
      <h2>Create drafts from Google Sheets</h2>
      <p className="lede">Import company rows to create one personalized, résumé-matched draft for each HR contact.</p>
      <form onSubmit={importSheet} className="upload-form">
        <div className="field"><label htmlFor="sheet-url">Google Sheet link or spreadsheet ID</label><input id="sheet-url" required value={sheetForm.spreadsheet} onChange={e => setSheetForm({ ...sheetForm, spreadsheet: e.target.value })} placeholder="https://docs.google.com/spreadsheets/d/..." /></div>
        <div className="form-grid"><div className="field"><label htmlFor="sheet-range">Sheet range</label><input id="sheet-range" required value={sheetForm.range} onChange={e => setSheetForm({ ...sheetForm, range: e.target.value })} placeholder="Companies!A:C" /></div><div className="field"><label htmlFor="sheet-location">Default location (optional)</label><input id="sheet-location" value={sheetForm.location} onChange={e => setSheetForm({ ...sheetForm, location: e.target.value })} placeholder="Bangalore / Remote" /></div></div>
        <div className="notice">The first row must include <strong>Company</strong>, <strong>HR Email</strong> (or <strong>Email</strong>), and <strong>Position</strong>. Connect Google in Integrations first. Importing creates review drafts; it never sends mail automatically.</div>
        <div className="form-actions"><button className="button" disabled={importing}>{importing ? "Importing…" : "Create drafts from sheet"}</button></div>
      </form>
    </section>

    <section className="card card-pad form-card section">
      <h2>Manual application email</h2>
      <form onSubmit={create} className="upload-form">
        <div className="form-grid">
          <div className="field"><label>Company name</label><input required value={form.companyName} onChange={e => setForm({ ...form, companyName: e.target.value })} /></div>
          <div className="field"><label>HR email</label><input required type="email" value={form.hrEmail} onChange={e => setForm({ ...form, hrEmail: e.target.value })} placeholder="hr@company.com" /></div>
          <div className="field"><label>Position</label><input required value={form.position} onChange={e => setForm({ ...form, position: e.target.value })} placeholder="Java Developer" /></div>
          <div className="field"><label>Location</label><input value={form.location} onChange={e => setForm({ ...form, location: e.target.value })} placeholder="Bangalore / Remote" /></div>
        </div>
        <div className="field"><label>Job description (optional)</label><textarea value={form.jobDescription} onChange={e => setForm({ ...form, jobDescription: e.target.value })} placeholder="Paste the JD to request a tailored derived resume. Your master resume is never edited." /></div>
        <div className="form-actions"><button className="button" disabled={saving}>{saving ? "Creating draft…" : "Create review draft"}</button></div>
      </form>
    </section>

    <section className="section">
      <div className="section-head"><h2>Saved email drafts</h2><button className="text-button" onClick={load}>Refresh</button></div>
      {drafts.map(draft => {
        const editing = editingId === draft.applicationId;
        return <div className="card card-pad" style={{ marginBottom: 12 }} key={draft.applicationId}>
          <div className="section-head"><strong>{editing ? "Edit saved draft" : draft.subject}</strong><span className="status pending">{draft.status}</span></div>
          {editing ? <form onSubmit={event => saveEdit(event, draft.applicationId)} className="upload-form">
            <div className="field"><label htmlFor={`recipient-${draft.applicationId}`}>To</label><input id={`recipient-${draft.applicationId}`} required type="email" value={editForm.recipientEmail} onChange={e => setEditForm({ ...editForm, recipientEmail: e.target.value })} /></div>
            <div className="field"><label htmlFor={`subject-${draft.applicationId}`}>Subject</label><input id={`subject-${draft.applicationId}`} required maxLength={998} value={editForm.subject} onChange={e => setEditForm({ ...editForm, subject: e.target.value })} /></div>
            <div className="field"><label htmlFor={`body-${draft.applicationId}`}>Email body</label><textarea id={`body-${draft.applicationId}`} required value={editForm.body} onChange={e => setEditForm({ ...editForm, body: e.target.value })} /></div>
            <div className="form-actions"><button className="button" disabled={saving}>{saving ? "Saving…" : "Save changes"}</button><button type="button" className="text-button" onClick={cancelEditing} disabled={saving}>Cancel</button></div>
          </form> : <>
            <p className="lede">To: {draft.recipientEmail}</p>
            <p style={{ whiteSpace: "pre-wrap" }}>{draft.body}</p>
            <div className="notice">{draft.attachmentNote}</div>
            <div className="form-actions">
              {draft.status === "PENDING_APPROVAL" && <><button type="button" className="text-button" disabled={actionInProgress === draft.applicationId} onClick={() => beginEditing(draft)}>Edit draft</button><button type="button" className="button" disabled={draft.attachmentMode !== "MASTER_PDF" || actionInProgress === draft.applicationId} onClick={() => action(draft.applicationId, "approve-email")}>{actionInProgress === draft.applicationId ? "Approving…" : "Approve draft"}</button></>}
              {draft.status === "APPROVED" && <button type="button" className="button" disabled={actionInProgress === draft.applicationId} onClick={() => action(draft.applicationId, "send-email")}>{actionInProgress === draft.applicationId ? "Sending email…" : "Send email now"}</button>}
              {draft.status === "SENT" && <span className="status ready">Sent</span>}
            </div>
          </>}
        </div>;
      })}
    </section>
  </>;
}
