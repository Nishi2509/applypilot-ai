import "./globals.css";
import { AppShell } from "@/components/AppShell";

export const metadata = { title: "ApplyPilot AI", description: "Approval-first job application assistant" };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="en"><body><AppShell>{children}</AppShell></body></html>; }
