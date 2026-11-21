"use client";

import { useEffect, useState, type FormEvent } from "react";
import { api } from "@/lib/api";
import { getSession } from "@/lib/session";
import { EmptyState } from "@/components/EmptyState";

type Job = { id: string; companyName: string | null; title: string; location: string | null; workMode: string | null; source: string; observedStatus: string };
type JobList = { items: Job[]; total: number };
type Source = { id: string; provider: "GREENHOUSE" | "LEVER"; boardSlug: string; displayName: string };
type SearchResult = { sourcesSearched: number; jobsDiscovered: number; jobsUpdated: number; errors: string[] };
type SourceForm = { provider: "GREENHOUSE" | "LEVER"; boardSlug: string; displayName: string };
const sourceDefaults: SourceForm = { provider: "GREENHOUSE", boardSlug: "", displayName: "" };

export default function Jobs() {
  const [jobs, setJobs] = useState<JobList | null>(null);
  const [sources, setSources] = useState<Source[]>([]);
  const [form, setForm] = useState<SourceForm>(sourceDefaults);
  const [loading, setLoading] = useState(false);
  const [savingSource, setSavingSource] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [mounted, setMounted] = useState(false);
  const signedIn = mounted && Boolean(getSession());
  const load = () => {
    if (!getSession()) return;
    api<JobList>("/jobs?limit=50").then(setJobs).catch(() => setJobs(null));
    api<Source[]>("/job-sources").then(setSources).catch(() => setSources([]));
  };
  useEffect(() => { setMounted(true); load(); }, []);

  async function addSource(event: FormEvent) {
    event.preventDefault(); setSavingSource(true); setError(""); setMessage("");
    try {
      await api<Source>("/job-sources", { method: "POST", body: JSON.stringify(form) });
      setForm(sourceDefaults); setMessage("Company board added. You can search it now."); load();
    } catch (e) { setError(e instanceof Error ? e.message : "Could not add that company board."); }
    finally { setSavingSource(false); }
  }
  async function search() {
    setLoading(true); setError(""); setMessage("");
    try {
      const result = await api<SearchResult>("/job-sources/search", { method: "POST" });
      setMessage(`Search complete: ${result.jobsDiscovered} new roles and ${result.jobsUpdated} refreshed roles from ${result.sourcesSearched} company board${result.sourcesSearched === 1 ? "" : "s"}.`);
      if (result.errors.length) setError(result.errors.join(" · "));
      load();
    } catch (e) { setError(e instanceof Error ? e.message : "Could not search the configured boards."); }
    finally { setLoading(false); }
  }
  async function webSearch() {
    setLoading(true); setError(""); setMessage("");
    try {
      const result = await api<SearchResult>("/job-sources/web-search", { method: "POST" });
      setMessage(`Web search complete: ${result.jobsDiscovered} new leads and ${result.jobsUpdated} refreshed leads from ${result.sourcesSearched} role searches.`);
      if (result.errors.length) setError(result.errors.join(" · "));
      load();
    } catch (e) { setError(e instanceof Error ? e.message : "Could not run web search."); }
    finally { setLoading(false); }
  }
  async function removeSource(id: string) { await api<void>(`/job-sources/${id}`, { method: "DELETE" }); load(); }

  return <>
    <div className="topbar"><div><h1>Job Explorer</h1><p className="lede">Search published roles from company career boards or the configured web-search provider.</p></div><div className="action-group"><button className="button secondary" onClick={webSearch} disabled={loading || !signedIn}>{loading ? "Searching…" : "Search web"}</button><button className="button" onClick={search} disabled={loading || !signedIn || sources.length === 0}>{loading ? "Searching boards…" : "Search jobs now"}</button></div></div>
    {error && <div className="message error">{error}</div>}{message && <div className="message success">{message}</div>}
    <section className="section" style={{ marginTop: 0 }}><div className="section-head"><h2>Company sources</h2><span className="status pending">Published job boards only</span></div><div className="card card-pad"><p className="lede" style={{ marginTop: 0 }}>Add a company&apos;s Greenhouse or Lever board identifier. For example, use <strong>acme</strong> from <code>jobs.lever.co/acme</code>. <strong>Search web</strong> uses your saved role and location preferences through Tavily when its API key is configured.</p><form className="form-grid" onSubmit={addSource} style={{ marginTop: 16 }}><div className="field"><label htmlFor="provider">Provider</label><select id="provider" value={form.provider} onChange={e => setForm({ ...form, provider: e.target.value as "GREENHOUSE" | "LEVER" })}><option value="GREENHOUSE">Greenhouse</option><option value="LEVER">Lever</option></select></div><div className="field"><label htmlFor="company">Company name</label><input id="company" required maxLength={255} value={form.displayName} onChange={e => setForm({ ...form, displayName: e.target.value })} placeholder="Example Company" /></div><div className="field"><label htmlFor="board">Board identifier</label><input id="board" required pattern="[A-Za-z0-9_-]+" value={form.boardSlug} onChange={e => setForm({ ...form, boardSlug: e.target.value })} placeholder="acme" /></div><div className="field" style={{ alignSelf: "end" }}><button className="button secondary" disabled={savingSource}>{savingSource ? "Adding…" : "Add company board"}</button></div></form>{sources.length > 0 && <div className="source-list">{sources.map(source => <div className="source-row" key={source.id}><div><strong>{source.displayName}</strong><span>{source.provider === "GREENHOUSE" ? "Greenhouse" : "Lever"} · {source.boardSlug}</span></div><button className="button secondary danger" type="button" onClick={() => removeSource(source.id)}>Remove</button></div>)}</div>}</div></section>
    <section className="section"><div className="section-head"><h2>Discovered jobs</h2><button className="text-button" onClick={load}>Refresh list</button></div>{jobs?.items.length ? <div className="card"><div style={{ overflowX: "auto" }}><table style={{ width: "100%", borderCollapse: "collapse", minWidth: 650 }}><thead><tr>{["Role", "Company", "Location", "Work mode", "Source", "Status"].map(x => <th key={x} style={{ textAlign: "left", padding: 14, color: "#64748b", fontSize: ".78rem", borderBottom: "1px solid #e4e9f2" }}>{x}</th>)}</tr></thead><tbody>{jobs.items.map(job => <tr key={job.id}><td style={{ padding: 14, fontWeight: 700 }}>{job.title}</td><td style={{ padding: 14 }}>{job.companyName ?? "—"}</td><td style={{ padding: 14 }}>{job.location ?? "—"}</td><td style={{ padding: 14 }}>{job.workMode ?? "—"}</td><td style={{ padding: 14 }}>{job.source}</td><td style={{ padding: 14 }}><span className="status ready">{job.observedStatus}</span></td></tr>)}</tbody></table></div></div> : <EmptyState icon="⌕" title="No jobs discovered yet" href="/settings" action="Review search preferences">Add a permitted company board above, then select Search jobs now. No job-board scraping or automatic applications are used.</EmptyState>}</section>
  </>;
}
