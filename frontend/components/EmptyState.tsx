import Link from "next/link";
export function EmptyState({ icon, title, children, href, action }: { icon: string; title: string; children: React.ReactNode; href?: string; action?: string }) { return <div className="card empty"><div className="empty-icon">{icon}</div><h2>{title}</h2><p>{children}</p>{href && action && <Link className="button" href={href}>{action}</Link>}</div>; }
