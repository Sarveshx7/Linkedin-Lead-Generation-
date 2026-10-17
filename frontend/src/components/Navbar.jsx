import React from 'react';
import { 
  Compass, 
  LayoutDashboard, 
  Sliders, 
  History, 
  Database, 
  Play, 
  RefreshCw,
  Sparkles,
  ShieldCheck,
  Sun,
  Moon
} from 'lucide-react';

export default function Navbar({ activeTab, setActiveTab, systemStatus, onQuickRun, isRunning, theme = 'dark', onToggleTheme }) {
  const isSupabase = systemStatus?.database?.isSupabase;
  const dbConnected = systemStatus?.database?.connected;

  return (
    <header className="sticky top-0 z-40 w-full glass-panel border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          
          {/* Brand */}
          <div
            className="flex shrink-0 items-center gap-2 sm:gap-3 cursor-pointer"
            onClick={() => setActiveTab('dashboard')}
            aria-label="LeadForge dashboard"
          >
            <div className="h-9 w-24 shrink-0 overflow-hidden rounded-lg border border-slate-200/70 bg-white shadow-sm ring-1 ring-black/5 sm:h-10 sm:w-32">
              <img
                src="/theboredmonkey-logo.png"
                alt="TheBoredMonkey — Creations & Connections"
                className="h-full w-full object-cover object-center"
              />
            </div>
            <div>
              <div className="flex items-center space-x-1.5">
                <span className="font-extrabold text-lg text-white tracking-tight">LeadForge</span>
              </div>
              <p className="text-[10px] text-slate-400 font-medium">Powered by TheBoredMonkey</p>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center space-x-1 bg-slate-900/60 p-1 rounded-xl border border-slate-800">
            <button
              onClick={() => setActiveTab('dashboard')}
              className={`flex items-center space-x-2 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'dashboard'
                  ? 'bg-emerald-500 text-white shadow-md shadow-emerald-500/25'
                  : 'text-slate-300 hover:text-white hover:bg-slate-800/60'
              }`}
            >
              <LayoutDashboard className="w-4 h-4" />
              <span>Dashboard</span>
            </button>

            <button
              onClick={() => setActiveTab('leads')}
              className={`flex items-center space-x-2 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'leads'
                  ? 'bg-emerald-500 text-white shadow-md shadow-emerald-500/25'
                  : 'text-slate-300 hover:text-white hover:bg-slate-800/60'
              }`}
            >
              <Compass className="w-4 h-4" />
              <span>Leads</span>
            </button>

            <button
              onClick={() => setActiveTab('campaigns')}
              className={`flex items-center space-x-2 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'campaigns'
                  ? 'bg-emerald-500 text-white shadow-md shadow-emerald-500/25'
                  : 'text-slate-300 hover:text-white hover:bg-slate-800/60'
              }`}
            >
              <Sliders className="w-4 h-4" />
              <span>Campaigns</span>
            </button>

            <button
              onClick={() => setActiveTab('history')}
              className={`flex items-center space-x-2 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'history'
                  ? 'bg-emerald-500 text-white shadow-md shadow-emerald-500/25'
                  : 'text-slate-300 hover:text-white hover:bg-slate-800/60'
              }`}
            >
              <History className="w-4 h-4" />
              <span>Run Logs</span>
            </button>

            <button
              onClick={() => setActiveTab('settings')}
              className={`flex items-center space-x-2 px-3.5 py-2 rounded-lg text-xs font-semibold transition-all ${
                activeTab === 'settings'
                  ? 'bg-emerald-500 text-white shadow-md shadow-emerald-500/25'
                  : 'text-slate-300 hover:text-white hover:bg-slate-800/60'
              }`}
            >
              <Database className="w-4 h-4" />
              <span>Supabase / DB</span>
            </button>
          </nav>

          {/* Right Action & Database Status */}
          <div className="flex items-center space-x-3">
            <button
              type="button"
              onClick={onToggleTheme}
              className="theme-toggle grid h-9 w-9 shrink-0 place-items-center rounded-xl border border-slate-700/80 bg-slate-900/70 transition-all hover:bg-slate-800"
              aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
              aria-pressed={theme === 'light'}
              title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
            >
              {theme === 'dark' ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </button>

            {/* Database indicator */}
            <div 
              onClick={() => setActiveTab('settings')}
              className="hidden lg:flex items-center space-x-1.5 px-2.5 py-1 rounded-full text-xs cursor-pointer border transition-colors bg-slate-900 border-slate-700/80 hover:border-slate-600"
              title="Click to view Database settings"
            >
              <span className={`w-2 h-2 rounded-full ${dbConnected ? (isSupabase ? 'bg-emerald-400 animate-pulse' : 'bg-cyan-400') : 'bg-rose-500'}`} />
              <span className="text-[11px] font-medium text-slate-300">
                {isSupabase ? 'Supabase' : (dbConnected ? `${systemStatus?.database?.databaseProduct || 'Database'} connected` : 'DB Disconnected')}
              </span>
            </div>

            {/* Quick Run Button */}
            <button
              onClick={onQuickRun}
              disabled={isRunning}
              className={`flex items-center space-x-2 px-3.5 py-2 rounded-xl text-xs font-bold transition-all shadow-md ${
                isRunning
                  ? 'bg-slate-800 text-slate-400 cursor-not-allowed border border-slate-700'
                  : 'bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-white shadow-emerald-500/20 active:scale-95'
              }`}
            >
              {isRunning ? (
                <>
                  <RefreshCw className="w-3.5 h-3.5 animate-spin text-emerald-400" />
                  <span>Collecting...</span>
                </>
              ) : (
                <>
                  <Play className="w-3.5 h-3.5 fill-current" />
                  <span>Collect Leads</span>
                </>
              )}
            </button>
          </div>

        </div>
      </div>
    </header>
  );
}
