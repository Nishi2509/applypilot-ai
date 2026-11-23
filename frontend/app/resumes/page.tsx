"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { api, apiUpload, ApiError } from "@/lib/api";
import { getSession } from "@/lib/session";

type MasterResume = { id: string; versionNumber: number; pdfFileName: string; texFileName: string; targetRoles: string; uploadedAt: string };

export default function Resumes() {
  const [resumes, setResumes] = useState<MasterResume[]>([]);
  const [targetRoles, setTargetRoles] = useState("");
  const [roleInputs, setRoleInputs] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const pdfInput = useRef<HTMLInputElement>(null);
  const texInput = useRef<HTMLInputElement>(null);

  const load = () => {
    if (!getSession()) { setLoading(false); return; }
    api<MasterResume[]>("/resumes/master/all").then(all => { setResumes(all); setRoleInputs(Object.fromEntries(all.map(item => [item.id, item.targetRoles]))); }).catch(() => setResumes([])).finally(() => setLoading(false));
  };
  useEffect(load, []);

  async function upload(event: FormEvent) {
    event.preventDefault(); setMessage(""); setError("");
    const pdf = pdfInput.current?.files?.[0]; const tex = texInput.current?.files?.[0];
    if (!pdf || !tex) { setError("Select both the PDF resume and its original .tex source file."); return; }
    const form = new FormData(); form.append("pdf", pdf); form.append("tex", tex); form.append("targetRoles", targetRoles);
    setUploading(true);
    try {
      const saved = await apiUpload<MasterResume>("/resumes/master", form);
      setResumes(current => [saved, ...current.filter(item => item.id !== saved.id)]);
      setRoleInputs(current => ({ ...current, [saved.id]: saved.targetRoles }));
      setTargetRoles(""); setMessage(`Master resume version ${saved.versionNumber} saved securely.`);
      if (pdfInput.current) pdfInput.current.value = ""; if (texInput.current) texInput.current.value = "";
    } catch (e) { setError(e instanceof ApiError ? e.message : "Could not upload your master resume."); }
    finally { setUploading(false); }
  }

  async function saveRoles(id: string) {
    setError(""); setMessage("");
    try {
      const saved = await api<MasterResume>(`/resumes/master/${id}/roles`, { method: "PUT", body: JSON.stringify({ targetRoles: roleInputs[id] ?? "" }) });
      setResumes(current => current.map(item => item.id === id ? saved : item));
      setRoleInputs(current => ({ ...current, [id]: saved.targetRoles }));
      setMessage(`Resume version ${saved.versionNumber} matching keywords saved.`);
    } catch (e) { setError(e instanceof ApiError ? e.message : "Could not save resume matching keywords."); }
  }

  async function download(kind: "pdf" | "tex") {
    const session = getSession(); if (!session) return;
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1"}/resumes/master/${kind}`, { headers: { Authorization: `Bearer ${session.accessToken}` } });
    if (!response.ok) { setError("Could not download the stored master resume."); return; }
    const blob = await response.blob(); const url = URL.createObjectURL(blob); const link = document.createElement("a"); link.href = url; link.download = kind === "pdf" ? "master-resume.pdf" : "master-resume.tex"; link.click(); URL.revokeObjectURL(url);
  }

  if (loading) return <p className="lede">Loading your resume vault…</p>;
  if (!getSession()) return <><h1>Resume Studio</h1><p className="lede">Sign in to upload and safeguard your master resumes.</p></>;
  return <>
    <div className="topbar"><div><h1>Resume Studio</h1><p className="lede">Keep role-specific master PDFs and choose the right attachment automatically for each job.</p></div>{resumes.length > 0 && <span className="status ready">Resumes ready</span>}</div>
    {error && <div className="message error">{error}</div>}{message && <div className="message success">{message}</div>}
    <section className="card card-pad form-card"><h2>Upload master resume</h2><p className="lede">Upload a final PDF and matching <code>.tex</code> source. Add matching keywords to attach it to the right job position.</p><form onSubmit={upload} className="upload-form"><div className="field"><label htmlFor="master-pdf">Master resume PDF</label><input ref={pdfInput} id="master-pdf" type="file" accept="application/pdf,.pdf" required /></div><div className="field"><label htmlFor="master-tex">Matching LaTeX source</label><input ref={texInput} id="master-tex" type="file" accept="text/x-tex,.tex" required /></div><div className="field"><label htmlFor="target-roles">Matching job keywords</label><input id="target-roles" value={targetRoles} onChange={e => setTargetRoles(e.target.value)} placeholder="Java, Spring Boot or Node.js, Full Stack" /><small>Examples: “Java, Spring Boot”, “Node.js”, or “Full Stack”.</small></div><div className="notice">The résumé with the best matching keywords is attached when a new email draft is created. You still approve every email before it is sent.</div><div className="form-actions"><button className="button" disabled={uploading}>{uploading ? "Saving resume…" : "Save master resume"}</button></div></form></section>
    <section className="section"><div className="section-head"><h2>Master resume versions</h2></div>{resumes.length ? <div className="card card-pad"><div className="source-list">{resumes.map(item => <div className="source-row" key={item.id}><div><strong>Version {item.versionNumber}</strong><span>Uploaded {new Date(item.uploadedAt).toLocaleString()}</span></div><div style={{ minWidth: 280 }}><label htmlFor={`roles-${item.id}`}>Matching keywords</label><div className="action-group"><input id={`roles-${item.id}`} value={roleInputs[item.id] ?? ""} onChange={e => setRoleInputs(current => ({ ...current, [item.id]: e.target.value }))} placeholder="Java, Node.js, Full Stack" /><button type="button" className="button secondary" onClick={() => saveRoles(item.id)}>Save</button></div></div></div>)}</div><div className="form-actions"><button className="button secondary" onClick={() => download("pdf")}>Download latest PDF</button><button className="button secondary" onClick={() => download("tex")}>Download latest LaTeX</button></div></div> : <div className="card empty"><div className="empty-icon">▣</div><h2>No master resume yet</h2><p>Upload both files above, then add matching keywords for each résumé version.</p></div>}</section>
  </>;
}
