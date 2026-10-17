import React, { useState, useEffect } from 'react';
import Navbar from './components/Navbar';
import Dashboard from './pages/Dashboard';
import LeadDiscovery from './pages/LeadDiscovery';
import CampaignManager from './pages/CampaignManager';
import CollectionHistory from './pages/CollectionHistory';
import SystemSettings from './pages/SystemSettings';
import { getDashboardStats, getSystemStatus, triggerGlobalRun } from './api';

export default function App() {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [theme, setTheme] = useState(() => {
    try {
      return localStorage.getItem('leadpulse-theme') === 'light' ? 'light' : 'dark';
    } catch {
      return 'dark';
    }
  });
  const [stats, setStats] = useState(null);
  const [systemStatus, setSystemStatus] = useState(null);
  const [isRunning, setIsRunning] = useState(false);
  const [collectionFeedback, setCollectionFeedback] = useState(null);

  useEffect(() => {
    document.documentElement.style.colorScheme = theme;
    try {
      localStorage.setItem('leadpulse-theme', theme);
    } catch (err) {
      console.warn('Could not save the selected theme:', err);
    }
  }, [theme]);

  const fetchAppData = async () => {
    try {
      const [statsData, statusData] = await Promise.all([
        getDashboardStats().catch(() => null),
        getSystemStatus().catch(() => null),
      ]);
      if (statsData) {
        setStats(statsData);

        const latestRun = statsData.recentRuns?.[0];
        if (latestRun?.status === 'COMPLETED') {
          setCollectionFeedback((current) => {
            const completedAt = Date.parse(latestRun.endTime || '');
            const completedAfterFailure = Number.isFinite(completedAt) && completedAt > Number(current?.failedAt);
            const discovered = Number(latestRun.leadsDiscovered || 0);
            const duplicates = Number(latestRun.duplicatesSkipped || 0);

            if (current?.kind === 'error') {
              if (!completedAfterFailure) return current;
            } else if (!(current?.title === 'No matching posts found' && !current.reportedAt && duplicates > 0)) {
              return current;
            }

            if (discovered === 0 && duplicates > 0) {
              return {
                kind: 'info',
                title: 'Collection completed — no new leads',
                message: `${duplicates} matching post${duplicates === 1 ? ' was' : 's were'} already in the dashboard and skipped as duplicates.`,
                details: null,
                reportedAt: Date.now(),
              };
            }

            return {
              kind: discovered > 0 ? 'success' : 'info',
              title: discovered > 0 ? 'Lead collection finished' : 'Collection run completed',
              message: discovered > 0
                ? `Added ${discovered} lead${discovered === 1 ? '' : 's'} from the latest collection run.`
                : 'The latest collection run completed. Existing matching leads were kept and duplicates were skipped.',
              details: null,
            };
          });
        }
      }
      if (statusData) setSystemStatus(statusData);
    } catch (err) {
      console.error('Failed to load initial data:', err);
    }
  };

  useEffect(() => {
    fetchAppData();
    const interval = setInterval(fetchAppData, 20000); // 20s periodic refresh
    return () => clearInterval(interval);
  }, []);

  const handleGlobalRun = async (campaignId = null) => {
    setIsRunning(true);
    setCollectionFeedback({ kind: 'running', message: 'Sending active campaign queries to configured search providers and comparing public LinkedIn posts…' });
    try {
      const run = await triggerGlobalRun(campaignId);
      await fetchAppData();
      const logs = run?.errorLogs || '';
      const linkedinNotConfigured = logs.includes('LinkedIn search is not configured')
        || logs.includes('Collector LINKEDIN is not configured');

      if (linkedinNotConfigured) {
        setCollectionFeedback({
          kind: 'error',
          title: 'LinkedIn collection is not configured',
          message: 'The run finished without collecting leads because no LinkedIn search provider has a backend API key.',
          details: 'Set TAVILY_API_KEY, EXA_API_KEY, or SERPER_API_KEY on the backend, then restart it. You can review the provider setup in System Settings.',
        });
      } else if (run?.status === 'FAILED') {
        setCollectionFeedback({
          kind: 'error',
          title: 'Lead collection failed',
          message: logs.split('\n').filter((line) => line.includes('[ERROR]') || line.includes('[WARN]')).slice(-3).join(' ') || 'The collection run failed. See Run Logs for details.',
          details: 'Open Run Logs to inspect this run and its source messages.',
        });
      } else {
        const discovered = Number(run?.leadsDiscovered || 0);
        const duplicates = Number(run?.duplicatesSkipped || 0);
        if (discovered > 0) {
          setCollectionFeedback({
            kind: 'success',
            title: 'Lead collection finished',
            message: `Added ${discovered} lead${discovered === 1 ? '' : 's'} from ${run?.sourcesAttempted || 0} active campaign${run?.sourcesAttempted === 1 ? '' : 's'}.`,
            details: null,
          });
        } else if (duplicates > 0) {
          setCollectionFeedback({
            kind: 'info',
            title: 'Collection completed — no new leads',
            message: `${duplicates} matching post${duplicates === 1 ? ' was' : 's were'} already in the dashboard and skipped as duplicates.`,
            details: null,
            reportedAt: Date.now(),
          });
        } else {
          setCollectionFeedback({
            kind: 'info',
            title: 'No matching posts found',
            message: 'The providers returned no posts that passed the LinkedIn URL, campaign keyword, exclusion, and commercial-intent filters. Try broadening the active campaign keywords.',
            details: null,
            reportedAt: Date.now(),
          });
        }
      }
    } catch (err) {
      console.error('Failed to trigger collection run:', err);
      setCollectionFeedback({
        kind: 'error',
        title: 'Could not start lead collection',
        message: err.response?.data?.message || err.message || 'The backend did not respond to the collection request.',
        details: 'Check that the backend is running, then open Run Logs or try again.',
        failedAt: Date.now(),
      });
    } finally {
      setIsRunning(false);
    }
  };

  return (
    <div
      className={`leadpulse-theme ${theme === 'light' ? 'theme-light' : 'theme-dark'} min-h-screen bg-slate-950 text-slate-100 flex flex-col`}
      onPointerMove={(event) => {
        if (event.pointerType !== 'mouse') return;
        const style = event.currentTarget.style;
        style.setProperty('--cursor-x', `${event.clientX}px`);
        style.setProperty('--cursor-y', `${event.clientY}px`);
        style.setProperty('--cursor-glow-opacity', '1');
      }}
      onPointerLeave={(event) => event.currentTarget.style.setProperty('--cursor-glow-opacity', '0')}
    >
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        systemStatus={systemStatus}
        onQuickRun={() => handleGlobalRun()}
        isRunning={isRunning}
        theme={theme}
        onToggleTheme={() => setTheme((current) => current === 'dark' ? 'light' : 'dark')}
      />

      {collectionFeedback && (
        <section
          role={collectionFeedback.kind === 'error' ? 'alert' : 'status'}
          aria-live="polite"
          className={`mx-auto mt-4 flex w-[calc(100%-2rem)] max-w-7xl items-start justify-between gap-4 rounded-xl border px-4 py-3 text-sm ${
            collectionFeedback.kind === 'error'
              ? 'border-rose-500/30 bg-rose-500/10 text-rose-100'
              : collectionFeedback.kind === 'success'
                ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-100'
                : collectionFeedback.kind === 'running'
                  ? 'border-cyan-500/30 bg-cyan-500/10 text-cyan-100'
                  : 'border-amber-500/30 bg-amber-500/10 text-amber-100'
          }`}
        >
          <div className="min-w-0">
            <p className="font-bold">{collectionFeedback.title || collectionFeedback.message}</p>
            {collectionFeedback.title && <p className="mt-1 text-xs opacity-90">{collectionFeedback.message}</p>}
            {collectionFeedback.details && <p className="mt-1 text-xs opacity-75">{collectionFeedback.details}</p>}
            {collectionFeedback.kind === 'error' && collectionFeedback.title?.includes('not configured') && (
              <button
                type="button"
                onClick={() => setActiveTab('settings')}
                className="mt-2 text-xs font-bold underline underline-offset-2 hover:text-white"
              >
                Open data source settings
              </button>
            )}
          </div>
          {collectionFeedback.kind !== 'running' && (
            <button
              type="button"
              aria-label="Dismiss collection result"
              onClick={() => setCollectionFeedback(null)}
              className="shrink-0 rounded px-2 py-1 text-xs font-bold opacity-70 hover:bg-white/10 hover:opacity-100"
            >
              Dismiss
            </button>
          )}
        </section>
      )}

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeTab === 'dashboard' && (
          <Dashboard
            stats={stats}
            systemStatus={systemStatus}
            onNavigate={setActiveTab}
            onTriggerRun={() => handleGlobalRun()}
            isRunning={isRunning}
          />
        )}

        {activeTab === 'leads' && (
          <LeadDiscovery />
        )}

        {activeTab === 'campaigns' && (
          <CampaignManager onTriggerRun={fetchAppData} />
        )}

        {activeTab === 'history' && (
          <CollectionHistory
            onTriggerRun={() => handleGlobalRun()}
            isRunning={isRunning}
          />
        )}

        {activeTab === 'settings' && (
          <SystemSettings systemStatus={systemStatus} />
        )}
      </main>
    </div>
  );
}
