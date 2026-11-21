"use client";

import { useEffect, useRef, useState, type FormEvent } from "react";
import { api, apiUpload, ApiError } from "@/lib/api";
import { getSession } from "@/lib/session";

type MasterResume = { id: string; versionNumber: number; pdfFileName: string; texFileName: string; uploadedAt: string };

export default function Resumes() {
  const [resume, setResume] = useState<MasterResume | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const pdfInput = useRef<HTMLInputElement>(null);
  const texInput = useRef<HTMLInputElement>(null);
  const load = () => {
    if (!getSession()) { setLoading(false); return; }
    api<MasterResume>("/resumes/master").then(setResume).catch(() => setResume(null)).finally(() => setLoading(false));
  };
  useEffect(load, []);

  async function upload(event: FormEvent) {
    event.preventDefault(); setMessage(""); setError("");
    const pdf = pdfInput.current?.files?.[0]; const tex = texInput.current?.files?.[0];
    if (!pdf || !tex) { setError("Select both the PDF resume and its original .tex source file."); return; }
    const form = new FormData(); form.append("pdf", pdf); form.append("tex", tex);
    setUploading(true);
    try {
      const saved = await apiUpload<MasterResume>("/resumes/master", form);
      setResume(saved); setMessage(`Master resume version ${saved.versionNumber} saved securely.`);
      if (pdfInput.current) pdfInput.current.value = ""; if (texInput.current) texInput.current.value = "";
    } catch (e) { setError(e instanceof ApiError ? e.message : "Could not upload your master resume."); }
    finally { setUploading(false); }
  }
  async function download(kind: "pdf" | "tex") {
    const session = getSession(); if (!session) return;
    const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1"}/resumes/master/${kind}`, { headers: { Authorization: `Bearer ${session.accessToken}` } });
    if (!response.ok) { setError("Could not download the stored master resume."); return; }
    const blob = await response.blob(); const url = URL.createObjectURL(blob); const link = document.createElement("a"); link.href = url; link.download = kind === "pdf" ? "master-resume.pdf" : "master-resume.tex"; link.click(); URL.revokeObjectURL(url);
  }

  if (loading) return <p className="lede">Loading your resume vault…</p>;
  if (!getSession()) return <><h1>Resume Studio</h1><p className="lede">Sign in to upload and safeguard your master resume.</p></>;
  return <>
    <div className="topbar"><div><h1>Resume Studio</h1><p className="lede">Keep one immutable master PDF and LaTeX source for every application workflow.</p></div>{resume && <span className="status ready">Master resume ready</span>}</div>
    {error && <div className="message error">{error}</div>}{message && <div className="message success">{message}</div>}
    <section className="card card-pad form-card"><h2>Upload master resume</h2><p className="lede">Upload the final PDF you want recruiters to receive and the matching original <code>.tex</code> file. Each upload creates an immutable version.</p><form onSubmit={upload} className="upload-form"><div className="field"><label htmlFor="master-pdf">Master resume PDF</label><input ref={pdfInput} id="master-pdf" type="file" accept="application/pdf,.pdf" required /></div><div className="field"><label htmlFor="master-tex">Matching LaTeX source</label><input ref={texInput} id="master-tex" type="file" accept="text/x-tex,.tex" required /></div><div className="notice">When a job description does not need a tailored resume, the latest master PDF is selected as the email attachment. Sending still requires your explicit approval.</div><div className="form-actions"><button className="button" disabled={uploading}>{uploading ? "Saving resume…" : "Save master resume"}</button></div></form></section>
    <section className="section"><div className="section-head"><h2>Current master resume</h2></div>{resume ? <div className="card card-pad"><div className="resume-meta"><div><strong>Version {resume.versionNumber}</strong><span>Uploaded {new Date(resume.uploadedAt).toLocaleString()}</span></div><div className="action-group"><button className="button secondary" onClick={() => download("pdf")}>Download PDF</button><button className="button secondary" onClick={() => download("tex")}>Download LaTeX</button></div></div></div> : <div className="card empty"><div className="empty-icon">▣</div><h2>No master resume yet</h2><p>Upload both files above. The original files are kept separate from all future job-specific versions.</p></div>}</section>
  </>;
}
