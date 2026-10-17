import React from 'react';
import { 
  Database, 
  CheckCircle, 
  AlertCircle, 
  ShieldCheck, 
  Key, 
  Rss, 
  Server, 
  Cpu, 
  Code,
  Terminal,
  ExternalLink
} from 'lucide-react';

export default function SystemSettings({ systemStatus }) {
  const db = systemStatus?.database;
  const collectors = systemStatus?.collectors || [];
  const linkedIn = collectors.find((collector) => collector.platform === 'LINKEDIN');
  const searchProviders = linkedIn?.searchProviders || [];
  const connectionLabel = !db?.connected
    ? 'Disconnected'
    : db.isSupabase
      ? 'Supabase Connected'
      : `${db.databaseProduct || 'Database'} Connected`;

  return (
    <div className="space-y-8 animate-fadeIn max-w-5xl mx-auto">
      <div>
        <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center space-x-2">
          <span>System & Data Source Configuration</span>
        </h1>
        <p className="text-xs text-slate-400">
          Database connection status and configured data sources
        </p>
      </div>

      {/* Database Connection Card */}
      <div className="p-6 rounded-2xl glass-card border border-slate-800 space-y-5">
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-3 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <Database className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white flex items-center space-x-2">
                <span>Database Connectivity</span>
                {db?.connected ? (
                  <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border flex items-center gap-1 ${
                    db.isSupabase
                      ? 'bg-emerald-500/15 text-emerald-400 border-emerald-500/30'
                      : 'bg-cyan-500/15 text-cyan-300 border-cyan-500/30'
                  }`}>
                    <CheckCircle className="w-3 h-3" /> {connectionLabel}
                  </span>
                ) : (
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/15 text-rose-400 border border-rose-500/30 flex items-center gap-1">
                    <AlertCircle className="w-3 h-3" /> {connectionLabel}
                  </span>
                )}
              </h2>
              <p className="text-xs text-slate-400">
                Mode: <strong className="text-slate-200">{db?.mode || 'Detecting...'}</strong>
              </p>
            </div>
          </div>

          <div className="text-right text-xs text-slate-400">
            <span>Engine: </span>
            <span className="font-mono text-slate-200">{db?.databaseProduct} {db?.databaseVersion}</span>
          </div>
        </div>

        {/* Supabase Connection Setup Box */}
        <div className="p-4 rounded-xl bg-slate-950 border border-slate-800/80 space-y-3">
          <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider flex items-center space-x-1.5">
            <Key className="w-4 h-4 text-emerald-400" />
            <span>{db?.isSupabase ? 'Supabase PostgreSQL' : 'Connect to Your Supabase Project'}</span>
          </h3>
          <p className="text-xs text-slate-300 leading-relaxed">
            {db?.isSupabase ? 'The backend is currently connected to Supabase PostgreSQL. Set these variables as secrets in your deployment environment.' : 'The backend is not connected to Supabase yet. Set these variables in the backend environment and restart it.'}
          </p>

          <div className="p-3 rounded-lg bg-slate-900 border border-slate-800 font-mono text-[11px] text-emerald-300 space-y-1">
            <div className="text-slate-500"># Set as backend environment variables or deployment secrets:</div>
            <div>SUPABASE_DB_URL=jdbc:postgresql://&lt;pooler-host&gt;:5432/postgres?sslmode=require</div>
            <div>SUPABASE_DB_USER=postgres.&lt;project-ref&gt;</div>
            <div>SUPABASE_DB_PASSWORD=&lt;your-supabase-db-password&gt;</div>
            <div>SUPABASE_DB_DRIVER=org.postgresql.Driver</div>
          </div>
        </div>
      </div>

      {/* LinkedIn Search Providers */}
      <div className="p-6 rounded-2xl glass-card border border-slate-800 space-y-4">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h3 className="text-sm font-bold text-white">LinkedIn public post search</h3>
            <p className="text-xs text-slate-400 mt-1">
              Sends each campaign query to every configured provider, compares overlapping posts, and filters out weak or irrelevant matches.
            </p>
          </div>
          <span className={`px-2 py-1 rounded text-[10px] font-bold ${
            linkedIn?.configured
              ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
              : 'bg-amber-500/10 text-amber-300 border border-amber-500/20'
          }`}>
            {linkedIn?.configured ? 'Configured' : 'Not configured'}
          </span>
        </div>
        <p className="text-xs text-slate-300 leading-relaxed">
          Add one, two, or all three API keys to the backend environment. Every configured provider receives the campaign keywords and exclusions. Results are deduplicated by exact LinkedIn post URL, filtered for campaign relevance and commercial intent, and tagged with the providers that returned them. Text and date come from provider results; structured author data is kept when supplied. Organization and location are not currently supplied by these adapters. Contact strings are extracted from returned text and need verification.
        </p>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {searchProviders.map((provider) => (
            <div key={provider.name} className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
              <div className="flex items-center justify-between gap-2">
                <span className="font-bold text-white text-xs">{provider.name}</span>
                <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${provider.configured ? 'bg-emerald-500/10 text-emerald-400' : 'bg-slate-800 text-slate-400'}`}>
                  {provider.configured ? 'Configured' : 'API key needed'}
                </span>
              </div>
              <p className="text-[11px] text-slate-400">{provider.freeAllowance}</p>
              <p className="text-[10px] text-slate-300">Backend variable: <code className="text-emerald-300">{provider.envVar}</code></p>
              <div className="flex gap-3 text-[10px]">
                <a href={provider.signupUrl} target="_blank" rel="noopener noreferrer" className="text-sky-400 hover:text-sky-300 inline-flex items-center gap-1">Get API key <ExternalLink className="w-3 h-3" /></a>
                <a href={provider.docsUrl} target="_blank" rel="noopener noreferrer" className="text-slate-400 hover:text-slate-200">API docs</a>
              </div>
            </div>
          ))}
        </div>
        <p className="text-[11px] text-slate-500">
          Use public search results only. The collector does not sign in to LinkedIn or call private LinkedIn endpoints.
        </p>
      </div>

      {/* Authorized Collectors Status */}
      <div className="p-6 rounded-2xl glass-card border border-slate-800 space-y-4">
        <div className="border-b border-slate-800/80 pb-3 flex items-center justify-between">
          <div>
            <h3 className="text-sm font-bold text-white flex items-center space-x-2">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Other Data Ingestion Adapters</span>
            </h3>
            <p className="text-xs text-slate-400">
              Public APIs and open syndication feeds run when enabled for a campaign
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
          {/* HackerNews */}
          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
            <div className="flex items-center justify-between">
              <span className="font-bold text-white text-xs">Hacker News Algolia Search API</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400">Authorized</span>
            </div>
            <p className="text-[11px] text-slate-400">
              Official public search API querying real-time hiring threads, monthly "Who is Hiring", and contract proposals.
            </p>
          </div>

          {/* Dev.to */}
          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
            <div className="flex items-center justify-between">
              <span className="font-bold text-white text-xs">Dev.to Syndication API & Feeds</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400">Authorized</span>
            </div>
            <p className="text-[11px] text-slate-400">
              RSS/Atom feeds and official REST endpoints for `#hiring`, `#freelance`, and Indian developer initiatives.
            </p>
          </div>

          {/* GitHub Search API */}
          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
            <div className="flex items-center justify-between">
              <span className="font-bold text-white text-xs">GitHub Public Search API</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400">Authorized</span>
            </div>
            <p className="text-[11px] text-slate-400">
              Official REST API searching public repository issues and RFPs with rate-limit header monitoring.
            </p>
          </div>

          {/* RSS Engine & Google Alerts */}
          <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2">
            <div className="flex items-center justify-between">
              <span className="font-bold text-white text-xs">Syndication Engine & Google Alerts</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400">Authorized</span>
            </div>
            <p className="text-[11px] text-slate-400">
              Ingests curated job feeds (WeWorkRemotely, RemoteOK) and custom Google Alerts RSS feeds for Indian keywords.
            </p>
          </div>
        </div>
      </div>

      {/* Compliance & Anti-Bot Philosophy */}
      <div className="p-6 rounded-2xl glass-card border border-slate-800 space-y-3">
        <h3 className="text-sm font-bold text-white flex items-center space-x-2">
          <ShieldCheck className="w-4 h-4 text-amber-400" />
          <span>Access Limitation & Data Quality Philosophy</span>
        </h3>
        <p className="text-xs text-slate-300 leading-relaxed">
          Unlike commercial scraping platforms that utilize residential proxy swarms and bypass CAPTCHAs, this local system adheres strictly to platform terms and web standards:
        </p>
        <ul className="text-xs text-slate-400 space-y-2 list-disc list-inside">
          <li><strong>No CAPTCHA Circumvention:</strong> Channels that require CAPTCHAs or Cloudflare challenge pages are not scraped without API credentials.</li>
          <li><strong>Rate Limit Backoff:</strong> All collectors maintain token pacing and honor HTTP 429 backoff headers.</li>
          <li><strong>Channel Isolation:</strong> A failure or rate-limit on one source does not crash the pipeline; other sources finish gracefully.</li>
          <li><strong>Deduplication Fingerprinting:</strong> Canonical URLs and content hashing (SHA-256) ensure clean database records without redundant entries.</li>
        </ul>
      </div>
    </div>
  );
}
