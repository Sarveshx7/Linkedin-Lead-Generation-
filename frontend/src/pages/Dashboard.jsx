import React, { useEffect, useState } from 'react';
import { 
  Users, 
  Flame, 
  CheckCircle, 
  Sliders, 
  Activity, 
  ArrowUpRight, 
  Compass, 
  Sparkles, 
  Clock, 
  Globe, 
  TrendingUp,
  ShieldCheck,
  RefreshCw,
  ExternalLink,
  Bookmark,
  XCircle,
  MessageSquare
} from 'lucide-react';
import LeadDetailModal from '../components/LeadDetailModal';
import { updateLeadStatus } from '../api';
import { getExactLinkedInPostUrl } from '../utils/sourceLinks';

function CountUp({ value, duration = 1200 }) {
  const target = Number.isFinite(Number(value)) ? Math.max(0, Math.round(Number(value))) : 0;
  const [displayedValue, setDisplayedValue] = useState(0);

  useEffect(() => {
    let animationFrame;
    if (target === 0) {
      setDisplayedValue(0);
      return undefined;
    }

    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
      setDisplayedValue(target);
      return undefined;
    }

    let startTime;
    const animate = (timestamp) => {
      if (startTime === undefined) startTime = timestamp;
      const progress = Math.min((timestamp - startTime) / duration, 1);
      const easedProgress = 1 - Math.pow(1 - progress, 3);
      setDisplayedValue(Math.round(target * easedProgress));
      if (progress < 1) animationFrame = window.requestAnimationFrame(animate);
    };

    animationFrame = window.requestAnimationFrame(animate);
    return () => window.cancelAnimationFrame(animationFrame);
  }, [target, duration]);

  return <span>{displayedValue.toLocaleString()}</span>;
}

function OrbitingLeadChips() {
  const [copyPhase, setCopyPhase] = useState(0);

  useEffect(() => {
    if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return undefined;
    const intervalId = window.setInterval(() => {
      setCopyPhase((current) => (current + 1) % 2);
    }, 8000);
    return () => window.clearInterval(intervalId);
  }, []);

  const chipContent = [
    [
      { title: 'Campaign match', detail: 'Qualified prospects', icon: <Users className="h-4 w-4" /> },
      { title: 'Brand fit', detail: 'Ready to review', icon: <Users className="h-4 w-4" /> },
    ],
    [
      { title: 'Lead signals', detail: 'Live discovery', icon: <Sparkles className="h-4 w-4" /> },
      { title: 'Fresh prospects', detail: 'Public post signals', icon: <Sparkles className="h-4 w-4" /> },
    ],
  ];

  return chipContent.map((phases, index) => {
    const content = phases[copyPhase];
    return (
      <div className="lead-hero-satellite" key={index}>
        <div className={`lead-hero-chip-anchor ${index === 0 ? 'top' : 'bottom'}`}>
          <div className="lead-hero-chip">
            <span className="lead-hero-chip-icon">{content.icon}</span>
            <span className="lead-hero-chip-copy" key={`${index}-${copyPhase}`}>
              <strong>{content.title}</strong>{content.detail}
            </span>
          </div>
        </div>
      </div>
    );
  });
}

export default function Dashboard({ stats, systemStatus, onNavigate, onTriggerRun, isRunning }) {
  const [selectedLead, setSelectedLead] = useState(null);
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [leadStatusUpdates, setLeadStatusUpdates] = useState({});

  const totalLeads = stats?.totalLeads || 0;
  const highIntent = stats?.highIntentLeads || 0;
  const indianLeads = stats?.indianMarketLeads || 0;
  const shortlisted = stats?.shortlistedLeads || 0;
  const activeCampaigns = stats?.activeCampaigns || 0;
  const categoryCounts = stats?.categoryCounts || {};
  const intentCounts = stats?.intentCounts || {};
  const platformCounts = stats?.platformCounts || {};
  const intentBreakdown = [
    { key: 'HIGH', label: 'High', count: intentCounts.HIGH || 0, color: 'bg-rose-400', text: 'text-rose-300' },
    { key: 'MEDIUM', label: 'Medium', count: intentCounts.MEDIUM || 0, color: 'bg-amber-400', text: 'text-amber-300' },
    { key: 'LOW', label: 'Low', count: intentCounts.LOW || 0, color: 'bg-slate-400', text: 'text-slate-300' },
  ];
  const intentTotal = intentBreakdown.reduce((sum, item) => sum + item.count, 0);
  const recentRuns = stats?.recentRuns || [];
  const recentLeads = (stats?.recentLeads || []).filter(
    (lead) => lead.sourcePlatform === 'LINKEDIN' && getExactLinkedInPostUrl(lead.sourceUrl)
  );
  const linkedInSearchReady = systemStatus?.collectors?.some(
    (collector) => collector.platform === 'LINKEDIN' && collector.configured
  ) || false;

  const handleUpdateStatus = async (leadId, newStatus, userNotes) => {
    try {
      const updated = await updateLeadStatus(leadId, newStatus, userNotes);
      setLeadStatusUpdates((current) => ({ ...current, [leadId]: updated }));
      if (selectedLead && selectedLead.id === leadId) {
        setSelectedLead(updated);
      }
    } catch (err) {
      console.error('Failed to update lead status:', err);
    }
  };

  const filteredLeads = selectedCategory === 'ALL'
    ? recentLeads
    : recentLeads.filter((l) => l.serviceCategory === selectedCategory);

  return (
    <div className="space-y-8 animate-fadeIn">
      {/* Hero Welcome & Quick Trigger */}
      <div className="lead-hero relative overflow-hidden rounded-3xl p-6 sm:p-8 glass-panel border border-slate-800 bg-gradient-to-r from-slate-900/90 via-slate-900/60 to-emerald-950/40">
        <div className="lead-hero-art hidden lg:block" aria-hidden="true">
          <div className="lead-hero-orbit" />
          <span className="lead-hero-spark one" />
          <span className="lead-hero-spark two" />
          <div className="lead-hero-core">
            <img src="/theboredmonkey-logo.png" alt="" className="lead-hero-core-logo" />
          </div>
          <OrbitingLeadChips />
        </div>
        <div className="relative z-10 flex flex-col items-start gap-6">
          <div className="space-y-3 max-w-2xl lg:max-w-[62%]">
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight leading-tight">
              Lead Intelligence & Discovery Engine
            </h1>
            <p className="text-slate-300 text-sm leading-relaxed max-w-xl">
              Searches public LinkedIn post indexes with campaign keywords, compares configured providers, and filters for qualified commercial intent.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <button
              onClick={() => onNavigate('leads')}
              className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold border border-slate-700 transition-all flex items-center space-x-2"
            >
              <Compass className="w-4 h-4 text-emerald-400" />
              <span>Browse All Leads</span>
            </button>

            <button
              onClick={onTriggerRun}
              disabled={isRunning}
              className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-white text-xs font-bold transition-all shadow-lg shadow-emerald-500/20 flex items-center space-x-2 active:scale-95 disabled:opacity-50"
            >
              {isRunning ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Searching...</span>
                </>
              ) : (
                <>
                  <TrendingUp className="w-4 h-4" />
                  <span>Collect Leads</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* KPI Stats Grid */}
      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-5">
        {/* Total Leads */}
        <div className="p-3 sm:p-5 rounded-2xl glass-card border border-slate-800 space-y-2 hover:border-slate-700 transition-all">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Fresh Leads · 7 Days</span>
            <Users className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold text-white">
            <CountUp value={totalLeads} />
          </div>
          <p className="text-[11px] text-slate-400">Posts with a provider-reported date</p>
        </div>

        {/* High Intent */}
        <div className="p-3 sm:p-5 rounded-2xl glass-card border border-slate-800 space-y-2 hover:border-rose-500/30 transition-all">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider text-rose-300">High Intent</span>
            <Flame className="w-4 h-4 text-rose-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold text-rose-400">
            <CountUp value={highIntent} />
          </div>
          <p className="text-[11px] text-slate-400">Direct hiring / budget ready</p>
        </div>

        {/* Indian Market */}
        <div className="p-3 sm:p-5 rounded-2xl glass-card border border-slate-800 space-y-2 hover:border-amber-500/30 transition-all">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider text-amber-300">India Signals</span>
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold text-amber-400">
            <CountUp value={indianLeads} />
          </div>
          <p className="text-[11px] text-slate-400">Explicit post wording; author location is not verified</p>
        </div>

        {/* Shortlisted */}
        <div className="p-3 sm:p-5 rounded-2xl glass-card border border-slate-800 space-y-2 hover:border-teal-500/30 transition-all">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider text-teal-300">Shortlisted</span>
            <CheckCircle className="w-4 h-4 text-teal-400" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold text-teal-300">
            <CountUp value={shortlisted} />
          </div>
          <p className="text-[11px] text-slate-400">Qualified for outreach</p>
        </div>

        {/* Active Campaigns */}
        <div className="col-span-2 p-3 sm:col-span-1 sm:p-5 rounded-2xl glass-card border border-slate-800 space-y-2 hover:border-slate-700 transition-all">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Campaigns</span>
            <Sliders className="w-4 h-4 text-slate-300" />
          </div>
          <div className="text-2xl sm:text-3xl font-extrabold text-slate-200">
            <CountUp value={activeCampaigns} />
          </div>
          <p className="text-[11px] text-slate-400">Active keyword trackers</p>
        </div>
      </div>

      {/* Analytics Distributions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Service Requirements Distribution */}
        <div className="p-6 rounded-2xl glass-card border border-slate-800 space-y-4 lg:col-span-2">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
            <h3 className="text-sm font-bold text-white flex items-center space-x-2">
              <Activity className="w-4 h-4 text-emerald-400" />
              <span>Leads by Service Requirement</span>
            </h3>
            <span className="text-xs text-slate-400">Auto-categorized by AI engine</span>
          </div>

          <div className="space-y-3 pt-2">
            {Object.keys(categoryCounts).length === 0 ? (
              <p className="text-xs text-slate-400 py-4 text-center">No categorized leads yet. Run a collection to populate.</p>
            ) : (
              Object.entries(categoryCounts).map(([cat, count]) => {
                const percentage = totalLeads > 0 ? Math.round((count / totalLeads) * 100) : 0;
                return (
                  <div key={cat} className="space-y-1">
                    <div className="flex justify-between text-xs">
                      <span className="font-medium text-slate-200">{cat.replaceAll('_', ' ')}</span>
                      <span className="text-slate-400 font-mono"><CountUp value={count} /> leads (<CountUp value={percentage} />%)</span>
                    </div>
                    <div className="w-full bg-slate-900 rounded-full h-2 overflow-hidden border border-slate-800">
                      <div 
                        className="bg-gradient-to-r from-emerald-500 to-teal-400 h-2 rounded-full transition-all duration-500"
                        style={{ width: `${Math.max(5, percentage)}%` }}
                      />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Source Channels & Intent Breakdown */}
        <div className="space-y-6">
          {/* Intent Breakdown */}
          <div className="p-6 rounded-2xl glass-card border border-slate-800 space-y-4">
            <div className="flex items-center justify-between gap-3 border-b border-slate-800/80 pb-3">
              <h3 className="text-sm font-bold text-white flex items-center space-x-2">
                <Flame className="w-4 h-4 text-rose-400" />
                <span>Commercial Intent Breakdown</span>
              </h3>
              <span className="shrink-0 text-[10px] font-medium text-slate-500"><CountUp value={intentTotal} /> leads</span>
            </div>

            <div className="space-y-4 pt-1">
              {intentBreakdown.map((item) => {
                const percentage = intentTotal > 0 ? Math.round((item.count / intentTotal) * 100) : 0;
                return (
                  <div key={item.key} className="space-y-1.5">
                    <div className="flex items-center justify-between text-xs">
                      <span className={`font-semibold ${item.text}`}>{item.label}</span>
                      <span className="font-mono text-slate-300"><CountUp value={item.count} /> <span className="text-slate-500">(<CountUp value={percentage} />%)</span></span>
                    </div>
                    <div
                      className="h-2.5 overflow-hidden rounded-full border border-slate-800 bg-slate-900/90"
                      role="img"
                      aria-label={`${item.label} intent: ${item.count} leads, ${percentage}%`}
                    >
                      <div className={`h-full rounded-full ${item.color} transition-all duration-500`} style={{ width: `${percentage}%` }} />
                    </div>
                  </div>
                );
              })}
              {intentTotal === 0 && (
                <p className="py-1 text-center text-xs text-slate-500">No intent data available yet.</p>
              )}
            </div>
          </div>

          {/* Sources Breakdown */}
          <div className="p-6 rounded-2xl glass-card border border-slate-800 space-y-3">
            <div className="border-b border-slate-800/80 pb-2">
              <h3 className="text-sm font-bold text-white flex items-center space-x-2">
                <Globe className="w-4 h-4 text-emerald-400" />
                <span>Ingestion Channels</span>
              </h3>
            </div>

            <div className="space-y-2 text-xs">
              {Object.entries(platformCounts).map(([platform, count]) => (
                <div key={platform} className="flex items-center justify-between py-1 border-b border-slate-800/50 last:border-0">
                  <span className="text-slate-300 font-medium">{platform}</span>
                    <span className="px-2 py-0.5 rounded bg-slate-800 text-emerald-400 font-mono font-bold">
                    <CountUp value={count} />
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Real-Time Discovered Leads Section */}
      <div className="p-6 rounded-2xl glass-card border border-slate-800 space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-4">
          <div className="space-y-1">
            <div className="flex items-center space-x-2">
              <h3 className="text-base font-bold text-white flex items-center space-x-2">
                <Sparkles className="w-4 h-4 text-emerald-400" />
                <span>High-Intent LinkedIn Leads</span>
              </h3>
              <span className={`flex items-center space-x-1 px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                linkedInSearchReady
                  ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                  : 'bg-amber-500/10 text-amber-300 border-amber-500/20'
              }`}>
                <span className={`w-1.5 h-1.5 rounded-full ${linkedInSearchReady ? 'bg-emerald-400' : 'bg-amber-400'}`}></span>
                <span>{linkedInSearchReady ? 'Provider search ready' : 'Provider keys needed'}</span>
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Showing recent LinkedIn posts classified as high intent. Review the source post before outreach; available details depend on provider results.
            </p>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={() => onNavigate('leads')}
              className="text-xs font-semibold text-emerald-400 hover:text-emerald-300 flex items-center space-x-1 px-3 py-1.5 rounded-xl bg-slate-900 border border-slate-800 hover:border-emerald-500/30 transition-all"
            >
              <span>View All (<CountUp value={totalLeads} />) Leads</span>
              <ArrowUpRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Category Filter Tabs */}
        <div className="flex flex-wrap items-center gap-2">
          {[
            { id: 'ALL', label: 'All Discovered' },
            { id: 'INFLUENCER_MARKETING', label: 'Influencer & Creators' },
            { id: 'ADVERTISING', label: 'Marketing & Advertising' },
            { id: 'PRODUCTION_HOUSE', label: 'Production House' },
            { id: 'BRAND_FILM', label: 'Brand Film' },
          ].map((cat) => (
            <button
              key={cat.id}
              onClick={() => setSelectedCategory(cat.id)}
              className={`px-3 py-1 rounded-xl text-xs font-semibold transition-all ${
                selectedCategory === cat.id
                  ? 'bg-emerald-500 text-slate-950 shadow-md shadow-emerald-500/20'
                  : 'bg-slate-900/80 hover:bg-slate-800 text-slate-400 hover:text-white border border-slate-800'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>

        {/* Leads List */}
        {filteredLeads.length === 0 ? (
          <div className="p-8 text-center text-xs text-slate-500">
            {linkedInSearchReady
              ? 'No high-intent LinkedIn leads in this category yet. Browse all leads to review other results, or adjust campaign keywords.'
              : 'Add at least one search provider key in System Settings, then click "Collect Leads".'}
          </div>
        ) : (
          <div className="overflow-x-auto rounded-xl border border-slate-800/80">
            <table className="w-full min-w-[850px] text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-900/50 text-slate-400">
                  <th className="px-4 py-3">Title &amp; Requirement</th>
                  <th className="px-3 py-3">Category</th>
                  <th className="px-3 py-3">Intent</th>
                  <th className="px-3 py-3">India signal</th>
                  <th className="px-3 py-3">Platform</th>
                  <th className="px-3 py-3">Date</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {filteredLeads.slice(0, 9).map((lead) => {
                  const rowLead = leadStatusUpdates[lead.id] || lead;
                  const leadDate = rowLead.publishedAt;
                  return (
                    <tr
                      key={rowLead.id}
                      onClick={() => setSelectedLead(rowLead)}
                      className="cursor-pointer transition-colors hover:bg-slate-800/40"
                    >
                      <td className="max-w-md px-4 py-3 font-semibold text-white">
                        <div className="truncate" title={rowLead.title || 'Untitled LinkedIn post'}>
                          {rowLead.title || 'Untitled LinkedIn post'}
                        </div>
                        <div className="truncate text-[11px] font-normal text-slate-400">
                          {rowLead.author ? `Name in result: ${rowLead.author}` : 'Name not returned by provider'}
                        </div>
                      </td>
                      <td className="px-3 py-3">
                        <span className="rounded border border-emerald-500/20 bg-emerald-500/10 px-2 py-0.5 text-[10px] text-emerald-400">
                          {rowLead.serviceCategory?.replaceAll('_', ' ') || 'Uncategorized'}
                        </span>
                      </td>
                      <td className="px-3 py-3">
                        <span className={`rounded px-2 py-0.5 text-[10px] font-bold ${
                          rowLead.intentLevel === 'HIGH' ? 'bg-rose-500/15 text-rose-400' :
                          rowLead.intentLevel === 'MEDIUM' ? 'bg-amber-500/15 text-amber-300' :
                          'bg-slate-800 text-slate-400'
                        }`}>
                          {rowLead.intentLevel || '—'}
                        </span>
                      </td>
                      <td className="px-3 py-3">
                        {rowLead.isIndianMarket
                          ? <span title="Explicit India-related wording in post; author location is not verified" className="font-bold text-amber-400">Yes</span>
                          : <span className="text-slate-500">No signal</span>}
                      </td>
                      <td className="px-3 py-3 font-mono text-slate-400">{rowLead.sourcePlatform || '—'}</td>
                      <td className="px-3 py-3 text-slate-400">
                        {leadDate ? new Date(leadDate).toLocaleDateString() : '—'}
                      </td>
                      <td className="px-4 py-3 text-right" onClick={(event) => event.stopPropagation()}>
                        <div className="inline-flex items-center gap-1">
                          <button
                            onClick={() => handleUpdateStatus(rowLead.id, 'SHORTLISTED', rowLead.userNotes)}
                            className={`rounded p-1 ${rowLead.reviewStatus === 'SHORTLISTED' ? 'text-emerald-400' : 'text-slate-500 hover:text-emerald-400'}`}
                            title="Shortlist"
                            aria-label={`Shortlist ${rowLead.title || 'lead'}`}
                          >
                            <Bookmark className="h-3.5 w-3.5" />
                          </button>
                          <button
                            onClick={() => handleUpdateStatus(rowLead.id, 'REVIEWED', rowLead.userNotes)}
                            className={`rounded p-1 ${rowLead.reviewStatus === 'REVIEWED' ? 'text-sky-400' : 'text-slate-500 hover:text-sky-400'}`}
                            title="Reviewed"
                            aria-label={`Mark ${rowLead.title || 'lead'} as reviewed`}
                          >
                            <CheckCircle className="h-3.5 w-3.5" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Recent Collection Activity */}
      <div className="p-6 rounded-2xl glass-card border border-slate-800 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
          <div className="space-y-0.5">
            <h3 className="text-sm font-bold text-white flex items-center space-x-2">
              <Clock className="w-4 h-4 text-emerald-400" />
              <span>Recent Collection History</span>
            </h3>
            <p className="text-xs text-slate-400">Execution audit trail, deduplication counts, and error tracking</p>
          </div>
          <button 
            onClick={() => onNavigate('history')}
            className="text-xs font-semibold text-emerald-400 hover:text-emerald-300 flex items-center space-x-1"
          >
            <span>View All Runs</span>
            <ArrowUpRight className="w-3.5 h-3.5" />
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="text-slate-400 border-b border-slate-800">
                <th className="py-2.5 px-3">Run ID</th>
                <th className="py-2.5 px-3">Campaign / Mode</th>
                <th className="py-2.5 px-3">Started</th>
                <th className="py-2.5 px-3">Discovered</th>
                <th className="py-2.5 px-3">Duplicates Skipped</th>
                <th className="py-2.5 px-3">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {recentRuns.slice(0, 5).map((run) => (
                <tr key={run.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="py-2.5 px-3 font-mono text-slate-400">#{run.id}</td>
                  <td className="py-2.5 px-3 font-medium text-white">{run.campaignName || 'Global Run'}</td>
                  <td className="py-2.5 px-3 text-slate-400">
                    {run.startTime ? new Date(run.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '-'}
                  </td>
                  <td className="py-2.5 px-3 font-mono text-emerald-400 font-bold">
                    +<CountUp value={run.leadsDiscovered} />
                  </td>
                  <td className="py-2.5 px-3 font-mono text-slate-400">
                    <CountUp value={run.duplicatesSkipped} />
                  </td>
                  <td className="py-2.5 px-3">
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      run.status === 'COMPLETED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' :
                      run.status === 'PARTIAL_SUCCESS' ? 'bg-amber-500/10 text-amber-300 border border-amber-500/20' :
                      run.status === 'RUNNING' ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20 animate-pulse' :
                      'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                    }`}>
                      {run.status}
                    </span>
                  </td>
                </tr>
              ))}
              {recentRuns.length === 0 && (
                <tr>
                  <td colSpan={6} className="py-6 text-center text-slate-500">
                    No collection runs executed yet. Click "Run Live Collection" to start!
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Lead Detail Modal */}
      {selectedLead && (
        <LeadDetailModal
          lead={selectedLead}
          onClose={() => setSelectedLead(null)}
          onUpdateStatus={handleUpdateStatus}
        />
      )}
    </div>
  );
}
