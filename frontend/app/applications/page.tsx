"use client";

import { useEffect, useState, type FormEvent } from "react";
import { api, ApiError } from "@/lib/api";
import { getSession } from "@/lib/session";
import { EmptyState } from "@/components/EmptyState";

type Status = "APPLIED" | "INTERVIEW" | "REJECTED" | "SUCCESS";
type Application = { id: string; companyName: string | null; position: string; location: string | null; status: Status; manualEntry: boolean; createdAt: string; updatedAt: string };
const statuses: Status[] = ["APPLIED", "INTERVIEW", "REJECTED", "SUCCESS"];
const emptyForm = { companyName: "", position: "", location: "", status: "APPLIED" as Status };

export default function Applications() {
  const [applications, setApplications] = useState<Application[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const load = () => { if (!getSession()) { setLoading(false); return; } api<Application[]>("/applications").then(setApplications).catch(() => setApplications([])).finally(() => setLoading(false)); };
  useEffect(load, []);
  async function submit(event: FormEvent) {
    event.preventDefault(); setSaving(true); setMessage(""); setError("");
    try { const record = await api<Application>("/applications/manual", { method: "POST", body: JSON.stringify(form) }); setApplications(current => [record, ...current]); setForm(emptyForm); setMessage("Manual application saved."); }
    catch (e) { setError(e instanceof ApiError ? e.message : "Could not save the application."); }
    finally { setSaving(false); }
  }
  async function updateStatus(id: string, status: Status) {
    setError("");
    try { const updated = await api<Application>(`/applications/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }); setApplications(current => current.map(item => item.id === id ? updated : item)); }
    catch (e) { setError(e instanceof ApiError ? e.message : "Could not update the application status."); }
  }
  if (loading) return <p className="lede">Loading your applications…</p>;
  if (!getSession()) return <><h1>Application Tracker</h1><p className="lede">Sign in to securely track applications you submit yourself.</p></>;
  return <>
    <div className="topbar"><div><h1>Application Tracker</h1><p className="lede">Keep a complete record of applications you submit manually or through ApplyPilot.</p></div><span className="status ready">Records are retained</span></div>
    {error && <div className="message error">{error}</div>}{message && <div className="message success">{message}</div>}
    <section className="card card-pad form-card"><h2>Add manual application</h2><p className="lede">Use this after applying through a company portal, referral, or another job site.</p><form onSubmit={submit} className="upload-form"><div className="form-grid"><div className="field"><label htmlFor="company-name">Company name</label><input id="company-name" required maxLength={255} value={form.companyName} onChange={e => setForm({ ...form, companyName: e.target.value })} placeholder="Company name" /></div><div className="field"><label htmlFor="position">Position</label><input id="position" required maxLength={500} value={form.position} onChange={e => setForm({ ...form, position: e.target.value })} placeholder="Java Backend Developer" /></div><div className="field"><label htmlFor="location">Location</label><input id="location" required maxLength={255} value={form.location} onChange={e => setForm({ ...form, location: e.target.value })} placeholder="Bangalore / Remote" /></div><div className="field"><label htmlFor="status">Status</label><select id="status" value={form.status} onChange={e => setForm({ ...form, status: e.target.value as Status })}>{statuses.map(status => <option key={status} value={status}>{label(status)}</option>)}</select></div></div><div className="form-actions"><button className="button" disabled={saving}>{saving ? "Saving…" : "Save application"}</button></div></form></section>
    <section className="section"><div className="section-head"><h2>All application records</h2><button className="text-button" onClick={load}>Refresh list</button></div>{applications.length ? <div className="card"><div style={{ overflowX: "auto" }}><table className="data-table"><thead><tr>{["Company", "Position", "Location", "Status", "Added", "Record"].map(label => <th key={label}>{label}</th>)}</tr></thead><tbody>{applications.map(application => <tr key={application.id}><td>{application.companyName ?? "—"}</td><td><strong>{application.position}</strong></td><td>{application.location ?? "—"}</td><td><select aria-label={`Status for ${application.position}`} value={application.status} onChange={e => updateStatus(application.id, e.target.value as Status)}>{statuses.map(status => <option key={status} value={status}>{label(status)}</option>)}</select></td><td>{new Date(application.createdAt).toLocaleDateString()}</td><td><span className="status">{application.manualEntry ? "Manual" : "ApplyPilot"}</span></td></tr>)}</tbody></table></div></div> : <EmptyState icon="◷" title="No applications tracked">Save the applications you submit manually above. Each status change is recorded in the application history.</EmptyState>}</section>
  </>;
}
function label(status: Status) { return status.charAt(0) + status.slice(1).toLowerCase(); }
