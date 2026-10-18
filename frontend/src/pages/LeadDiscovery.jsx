import React, { useState, useEffect } from 'react';
import { 
  Search, 
  Filter, 
  Download, 
  ExternalLink, 
  Flame, 
  CheckCircle, 
  Bookmark, 
  XCircle, 
  Calendar, 
  User, 
  Building, 
  MapPin, 
  RotateCw,
  Mail,
  Phone,
  LayoutGrid,
  List,
  Sparkles,
  ChevronLeft,
  ChevronRight,
  Trash2
} from 'lucide-react';
import { getLeads, updateLeadStatus, deleteLead, deleteAllLeads, exportLeadsCsv } from '../api';
import LeadDetailModal from '../components/LeadDetailModal';
import { getLeadSourceUrl } from '../utils/sourceLinks';

const extractContacts = (value) => {
  const text = typeof value === 'string' ? value : '';
  return {
    email: text.match(/[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}/)?.[0] || null,
    phone: text.match(/(?:\+?\d[\d\s().-]{7,}\d)/)?.[0]?.trim() || null,
  };
};

const formatLeadDate = (value) => value
  ? new Date(value).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
  : 'Date unavailable';

export default function LeadDiscovery() {
  const [leads, setLeads] = useState([]);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);

  // Filters state
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('');
  const [intent, setIntent] = useState('');
  const [status, setStatus] = useState('');
  const [platform, setPlatform] = useState('LINKEDIN');
  const [sortBy, setSortBy] = useState('intentLevel');
  const [sortDir, setSortDir] = useState('desc');
  const [page, setPage] = useState(0);
  const [viewMode, setViewMode] = useState('grid'); // 'grid' or 'table'
  const [deletingLeadId, setDeletingLeadId] = useState(null);
  const [isDeletingAll, setIsDeletingAll] = useState(false);

  // Selected lead for detail modal
  const [selectedLead, setSelectedLead] = useState(null);

  const fetchLeads = async () => {
    setIsLoading(true);
    try {
      const params = {
        page,
        size: 18,
        sortBy,
        sortDir,
        ...(query ? { query } : {}),
        ...(category ? { category } : {}),
        ...(intent ? { intent } : {}),
        ...(status ? { status } : {}),
        ...(platform ? { platform } : {}),
      };

      const data = await getLeads(params);
      setLeads(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load leads:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLeads();
  }, [page, sortBy, sortDir, category, intent, status, platform]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    fetchLeads();
  };

  const handleUpdateStatus = async (leadId, newStatus, userNotes) => {
    try {
      const updated = await updateLeadStatus(leadId, newStatus, userNotes);
      setLeads((prev) => prev.map((l) => (l.id === leadId ? updated : l)));
      if (selectedLead && selectedLead.id === leadId) {
        setSelectedLead(updated);
      }
    } catch (err) {
      console.error('Failed to update lead status:', err);
    }
  };

  const handleDeleteLead = async (lead) => {
    const title = lead.title?.trim() || 'this lead';
    if (!window.confirm(`Delete “${title}”? This cannot be undone.`)) return;

    setDeletingLeadId(lead.id);
    try {
      await deleteLead(lead.id);
      if (selectedLead?.id === lead.id) setSelectedLead(null);
      if (leads.length === 1 && page > 0) {
        setPage((currentPage) => Math.max(0, currentPage - 1));
      } else {
        await fetchLeads();
      }
    } catch (err) {
      console.error('Failed to delete lead:', err);
      window.alert('Could not delete this lead. Please try again.');
    } finally {
      setDeletingLeadId(null);
    }
  };

  const handleDeleteAllLeads = async () => {
    if (!window.confirm('Delete all scanned leads? This permanently removes every lead, including leads outside the current filters. Campaigns and run logs will remain.')) return;

    setIsDeletingAll(true);
    try {
      const result = await deleteAllLeads();
      setLeads([]);
      setTotalPages(1);
      setTotalElements(0);
      setPage(0);
      setSelectedLead(null);
      window.alert(`${(result?.deletedCount ?? 0).toLocaleString()} leads deleted.`);
    } catch (err) {
      console.error('Failed to delete all leads:', err);
      window.alert('Could not delete all leads. Please try again.');
    } finally {
      setIsDeletingAll(false);
    }
  };

  const handleExport = () => {
    exportLeadsCsv({
      ...(query ? { query } : {}),
      ...(category ? { category } : {}),
      ...(intent ? { intent } : {}),
      ...(status ? { status } : {}),
      ...(platform ? { platform } : {}),
      sortBy,
      sortDir,
    });
  };

  const handleClearFilters = () => {
    setQuery('');
    setCategory('');
    setIntent('');
    setStatus('');
    setPlatform('LINKEDIN');
    setPage(0);
  };

  const activeFilterCount = [query, category, intent, status, platform].filter(Boolean).length;

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header & Export */}
      <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">Leads</h1>
            <span className="inline-flex items-center rounded-full bg-violet-500/10 px-3 py-1 text-xs font-bold text-violet-300 border border-violet-400/20">
              {totalElements.toLocaleString()} leads
            </span>
          </div>
          <p className="max-w-2xl text-sm text-slate-400">
            Find, qualify, and organize public opportunities from across your discovery sources.
          </p>
        </div>

        <div className="flex flex-wrap items-center justify-end gap-2">
          <button
            type="button"
            onClick={handleDeleteAllLeads}
            disabled={isDeletingAll}
            className="inline-flex items-center gap-2 rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-2.5 text-xs font-semibold text-rose-300 shadow-sm transition-all hover:bg-rose-500/20 disabled:cursor-wait disabled:opacity-50"
          >
            <Trash2 className="h-4 w-4" />
            <span>{isDeletingAll ? 'Deleting…' : 'Delete all leads'}</span>
          </button>
          <div className="flex items-center bg-slate-900/80 border border-slate-800 rounded-xl p-1" role="group" aria-label="Lead layout">
            <button
              onClick={() => setViewMode('grid')}
              aria-label="Card view"
              aria-pressed={viewMode === 'grid'}
              className={`p-2 rounded-lg transition-colors ${
                viewMode === 'grid' ? 'bg-emerald-500 text-white shadow-sm' : 'text-slate-400 hover:text-white'
              }`}
            >
              <LayoutGrid className="h-4 w-4" />
            </button>
            <button
              onClick={() => setViewMode('table')}
              aria-label="Table view"
              aria-pressed={viewMode === 'table'}
              className={`p-2 rounded-lg transition-colors ${
                viewMode === 'table' ? 'bg-emerald-500 text-white shadow-sm' : 'text-slate-400 hover:text-white'
              }`}
            >
              <List className="h-4 w-4" />
            </button>
          </div>

          <button
            onClick={handleExport}
            className="inline-flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-800 px-4 py-2.5 text-xs font-semibold text-slate-200 shadow-sm transition-all hover:bg-slate-700"
          >
            <Download className="h-4 w-4 text-emerald-400" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <section className="lead-filter-panel glass-card rounded-2xl border border-slate-800 p-4 sm:p-6 space-y-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="grid h-10 w-10 place-items-center rounded-xl bg-violet-500/10 text-violet-300 border border-violet-400/20">
              <Filter className="h-4 w-4" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-white">Search and refine</h2>
              <p className="mt-0.5 text-xs text-slate-400">Use keywords and filters to narrow the results.</p>
            </div>
          </div>
          <span className="text-xs text-slate-400">
            {activeFilterCount ? `${activeFilterCount} filter${activeFilterCount === 1 ? '' : 's'} applied` : 'Showing all discovered leads'}
          </span>
        </div>

        {/* Search Input */}
        <form onSubmit={handleSearchSubmit} className="flex flex-col gap-2 sm:flex-row">
          <div className="relative flex-1">
            <Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search lead titles, organizations, keywords, or locations"
              className="w-full rounded-xl border border-slate-800 bg-slate-950 py-3 pl-11 pr-4 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:border-emerald-500 transition-colors"
            />
          </div>
          <button
            type="submit"
            className="rounded-xl bg-emerald-500 px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-emerald-400"
          >
            Search
          </button>
        </form>

        {/* Filter Selectors */}
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 text-sm">
          {/* Service Category */}
          <select
            value={category}
            onChange={(e) => { setCategory(e.target.value); setPage(0); }}
            className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-slate-200 focus:outline-none focus:border-emerald-500"
          >
            <option value="">All Services</option>
            <option value="INFLUENCER_MARKETING">Influencer Marketing</option>
            <option value="ADVERTISING">Advertising & Media</option>
            <option value="PRODUCTION_HOUSE">Production House</option>
            <option value="BRAND_FILM">Brand Film</option>
            <option value="OTHER">Other Services</option>
          </select>

          {/* Intent Level */}
          <select
            value={intent}
            onChange={(e) => { setIntent(e.target.value); setPage(0); }}
            className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-slate-200 focus:outline-none focus:border-emerald-500"
          >
            <option value="">All Intent Levels</option>
            <option value="HIGH">High Intent</option>
            <option value="MEDIUM">Medium Intent</option>
            <option value="LOW">Informational</option>
          </select>

          {/* Review Status */}
          <select
            value={status}
            onChange={(e) => { setStatus(e.target.value); setPage(0); }}
            className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-slate-200 focus:outline-none focus:border-emerald-500"
          >
            <option value="">All Statuses</option>
            <option value="DISCOVERED">Discovered (New)</option>
            <option value="SHORTLISTED">Shortlisted</option>
            <option value="REVIEWED">Reviewed</option>
            <option value="REJECTED">Rejected</option>
          </select>

          {/* Platform */}
          <select
            value={platform}
            onChange={(e) => { setPlatform(e.target.value); setPage(0); }}
            className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-slate-200 focus:outline-none focus:border-emerald-500"
          >
            <option value="LINKEDIN">LinkedIn (Posts & RFPs)</option>
          </select>

          {/* Sorting */}
          <select
            value={`${sortBy}-${sortDir}`}
            onChange={(e) => {
              const [sb, sd] = e.target.value.split('-');
              setSortBy(sb);
              setSortDir(sd);
              setPage(0);
            }}
            className="bg-slate-950 border border-slate-800 rounded-xl px-3 py-2 text-slate-200 focus:outline-none focus:border-emerald-500"
          >
            <option value="discoveredAt-desc">Newest First</option>
            <option value="relevanceScore-desc">Highest Relevance</option>
            <option value="intentLevel-desc">Highest Intent</option>
            <option value="discoveredAt-asc">Oldest First</option>
          </select>

          <div className="flex items-center gap-2 rounded-xl border border-amber-500/20 bg-amber-500/5 px-3 py-2 text-xs font-semibold text-amber-300" title="Only posts with explicit India-related wording are included.">
            <MapPin className="h-4 w-4" />
            <span>India-only results</span>
          </div>
        </div>

        {/* Optional shortcuts stay collapsed so the filters remain easy to scan. */}
        <details className="lead-shortcuts border-t border-slate-800/60 pt-4">
          <summary className="flex cursor-pointer list-none items-center justify-between gap-3 text-sm font-semibold text-slate-300 hover:text-white">
            <span className="flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-sky-400" />
              Live LinkedIn search shortcuts
            </span>
            <span className="text-xs font-normal text-slate-500">6 public searches</span>
          </summary>
          <div className="mt-3 flex flex-wrap items-center gap-2">

          <a
            href={`https://www.linkedin.com/search/results/content/?keywords=${encodeURIComponent('"influencer marketing"')}&origin=GLOBAL_SEARCH_HEADER`}
            target="_blank"
            rel="noopener noreferrer"
            className="px-2.5 py-1 rounded-lg bg-sky-950/40 hover:bg-sky-900/60 border border-sky-500/30 text-sky-300 text-[11px] font-semibold flex items-center space-x-1 transition-all"
            title="Search LinkedIn for influencer marketing posts"
          >
            <span>Influencer Marketing</span>
            <ExternalLink className="w-3 h-3" />
          </a>

          <a
            href={`https://www.linkedin.com/search/results/content/?keywords=${encodeURIComponent('"production house"')}&origin=GLOBAL_SEARCH_HEADER`}
            target="_blank"
            rel="noopener noreferrer"
            className="px-2.5 py-1 rounded-lg bg-sky-950/40 hover:bg-sky-900/60 border border-sky-500/30 text-sky-300 text-[11px] font-semibold flex items-center space-x-1 transition-all"
            title="Search LinkedIn for production house posts"
          >
            <span>Production House</span>
            <ExternalLink className="w-3 h-3" />
          </a>

          <a
            href={`https://www.linkedin.com/search/results/content/?keywords=${encodeURIComponent('"advertising agency"')}&origin=GLOBAL_SEARCH_HEADER`}
            target="_blank"
            rel="noopener noreferrer"
            className="px-2.5 py-1 rounded-lg bg-sky-950/40 hover:bg-sky-900/60 border border-sky-500/30 text-sky-300 text-[11px] font-semibold flex items-center space-x-1 transition-all"
            title="Search LinkedIn for advertising agency posts"
          >
            <span>Advertising</span>
            <ExternalLink className="w-3 h-3" />
          </a>

          <a
            href={`https://www.linkedin.com/search/results/content/?keywords=${encodeURIComponent('"brand film"')}&origin=GLOBAL_SEARCH_HEADER`}
            target="_blank"
            rel="noopener noreferrer"
            className="px-2.5 py-1 rounded-lg bg-sky-950/40 hover:bg-sky-900/60 border border-sky-500/30 text-sky-300 text-[11px] font-semibold flex items-center space-x-1 transition-all"
            title="Search LinkedIn for brand film posts"
          >
            <span>Brand Film</span>
            <ExternalLink className="w-3 h-3" />
          </a>

          <a
            href={`https://www.linkedin.com/search/results/content/?keywords=${encodeURIComponent('"brand sponsorship" OR "creator collaboration"')}&origin=GLOBAL_SEARCH_HEADER`}
            target="_blank"
            rel="noopener noreferrer"
            className="px-2.5 py-1 rounded-lg bg-indigo-950/40 hover:bg-indigo-900/60 border border-indigo-500/30 text-indigo-300 text-[11px] font-semibold flex items-center space-x-1 transition-all"
            title="Search LinkedIn for brand sponsorship and creator collaboration posts"
          >
            <span>Brand Sponsorship</span>
            <ExternalLink className="w-3 h-3" />
          </a>

          <a
            href={`https://www.linkedin.com/search/results/content/?keywords=${encodeURIComponent('("influencer marketing" OR "production house" OR "advertising agency" OR "brand film")')}&origin=GLOBAL_SEARCH_HEADER`}
            target="_blank"
            rel="noopener noreferrer"
            className="px-2.5 py-1 rounded-lg bg-emerald-950/40 hover:bg-emerald-900/60 border border-emerald-500/30 text-emerald-300 text-[11px] font-semibold flex items-center space-x-1 transition-all ml-auto"
            title="Search LinkedIn across all 4 target categories"
          >
            <span>All 4 Categories</span>
            <ExternalLink className="w-3 h-3" />
          </a>
          </div>
        </details>

        {/* Clear Filters helper */}
        {(query || category || intent || status || platform) && (
          <div className="flex items-center justify-between text-xs pt-1 text-slate-400">
            <span>Filtered results active</span>
            <button
              onClick={handleClearFilters}
              className="text-emerald-400 hover:text-emerald-300 font-semibold underline"
            >
              Reset All Filters
            </button>
          </div>
        )}
      </section>

      {/* Leads Container */}
      {isLoading ? (
        <div className="py-20 text-center space-y-3">
          <RotateCw className="w-8 h-8 text-emerald-400 animate-spin mx-auto" />
          <p className="text-xs text-slate-400 font-medium">Querying and ranking leads...</p>
        </div>
      ) : leads.length === 0 ? (
        <div className="p-12 text-center rounded-2xl glass-card border border-slate-800 space-y-3">
          <p className="text-slate-300 font-semibold">No matching leads found</p>
          <p className="text-xs text-slate-400 max-w-md mx-auto">
            Try adjusting your search filters or click "Collect Leads" to search the active providers again.
          </p>
        </div>
      ) : viewMode === 'grid' ? (
        /* Grid Cards View */
        <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
          {leads.map((lead) => {
            const contacts = extractContacts(lead.contactInfo);
            const relevance = Math.min(100, Math.max(0, Number(lead.relevanceScore) || 0));
            const isIndexedLinkedIn = lead.sourcePlatform === 'LINKEDIN';
            const sourceUrl = getLeadSourceUrl(lead);
            return (
              <article key={lead.id} className="lead-result-card glass-card flex min-h-[370px] flex-col rounded-2xl border border-slate-800 p-5 sm:p-6">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="lead-category-badge rounded-full border px-3 py-1 text-[11px] font-bold">
                      {lead.serviceCategory?.replaceAll('_', ' ') || 'GENERAL'}
                    </span>
                    <span className="rounded-full border border-slate-700/70 bg-slate-900/60 px-2.5 py-1 text-[10px] font-semibold text-slate-400">
                      {lead.sourcePlatform || 'Unknown source'}
                    </span>
                  </div>
                  <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[10px] font-bold ${
                    lead.intentLevel === 'HIGH' ? 'bg-rose-500/10 text-rose-400' :
                    lead.intentLevel === 'MEDIUM' ? 'bg-amber-500/10 text-amber-300' :
                    'bg-slate-800/80 text-slate-400'
                  }`}>
                    {lead.intentLevel === 'HIGH' && <Flame className="h-3 w-3" />}
                    {lead.intentLevel || 'LOW'} INTENT
                  </span>
                </div>

                <div className="mt-4 space-y-2">
                  <button
                    type="button"
                    onClick={() => setSelectedLead(lead)}
                    className="lead-result-title block w-full text-left text-lg font-bold leading-snug text-white transition-colors hover:text-violet-300"
                  >
                    {lead.title || 'Untitled opportunity'}
                  </button>
                  <p className="line-clamp-3 text-sm leading-6 text-slate-300">
                    {lead.content || 'No post summary is available. Open the lead details to review the source information.'}
                  </p>
                  {isIndexedLinkedIn && <span className="text-[10px] font-medium uppercase tracking-wider text-slate-500">Search result text · verify on source</span>}
                </div>

                <div className="lead-facts mt-5 grid grid-cols-1 gap-3 rounded-xl border border-slate-800/80 bg-slate-950/40 p-3 sm:grid-cols-3">
                  <div className="min-w-0">
                    <span className="mb-1 block text-[10px] font-semibold uppercase tracking-wider text-slate-500">{isIndexedLinkedIn ? 'Name in search result' : 'Posted by'}</span>
                    <div className="flex min-w-0 items-center gap-1.5 text-xs font-semibold text-slate-200">
                      <User className="h-3.5 w-3.5 shrink-0 text-slate-400" />
                      {lead.authorProfileUrl ? (
                        <a href={lead.authorProfileUrl} target="_blank" rel="noopener noreferrer" className="truncate hover:text-violet-300">
                          {lead.author || 'View author'}
                        </a>
                      ) : <span className="truncate">{lead.author || (isIndexedLinkedIn ? 'Not returned' : 'Anonymous')}</span>}
                    </div>
                  </div>
                  <div className="min-w-0">
                    <span className="mb-1 block text-[10px] font-semibold uppercase tracking-wider text-slate-500">{isIndexedLinkedIn ? 'Organization returned' : 'Organization'}</span>
                    <div className="flex min-w-0 items-center gap-1.5 text-xs font-semibold text-slate-200">
                      <Building className="h-3.5 w-3.5 shrink-0 text-slate-400" />
                      <span className="truncate">{lead.company || (isIndexedLinkedIn ? 'Not returned' : 'Not specified')}</span>
                    </div>
                  </div>
                  <div className="min-w-0">
                    <span className="mb-1 block text-[10px] font-semibold uppercase tracking-wider text-slate-500">{isIndexedLinkedIn ? 'Market signal' : 'Location'}</span>
                    <div className="flex min-w-0 items-center gap-1.5 text-xs font-semibold text-slate-200">
                      <MapPin className="h-3.5 w-3.5 shrink-0 text-slate-400" />
                      <span className="truncate">{isIndexedLinkedIn ? (lead.isIndianMarket ? 'Explicit India post signal' : 'No explicit India signal') : (lead.location || 'Not specified')}</span>
                    </div>
                  </div>
                </div>

                {(contacts.email || contacts.phone || lead.isIndianMarket) && (
                  <div className="mt-3 flex flex-wrap items-center gap-2">
                    {isIndexedLinkedIn && (contacts.email || contacts.phone) && <span className="w-full text-[10px] text-slate-500">Contact found in search text; verify before outreach.</span>}
                    {contacts.email && (
                      <a href={`mailto:${contacts.email}`} className="lead-contact-link inline-flex max-w-full items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-xs font-medium">
                        <Mail className="h-3.5 w-3.5 shrink-0" /><span className="truncate">{contacts.email}</span>
                      </a>
                    )}
                    {contacts.phone && (
                      <a href={`tel:${contacts.phone}`} className="lead-contact-link inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1.5 text-xs font-medium">
                        <Phone className="h-3.5 w-3.5" /><span>{contacts.phone}</span>
                      </a>
                    )}
                    {lead.isIndianMarket && (
                      <span title="Inferred from explicit wording in the post; this does not verify the author’s location." className="rounded-lg border border-amber-500/20 bg-amber-500/10 px-2.5 py-1.5 text-xs font-semibold text-amber-300">India signal</span>
                    )}
                  </div>
                )}

                <div className="mt-auto pt-5">
                  <div className="mb-3 flex items-center justify-between gap-3 text-[11px] text-slate-400">
                    <span className="inline-flex items-center gap-1.5" title={isIndexedLinkedIn ? 'Date reported by the search provider' : 'Source date'}><Calendar className="h-3.5 w-3.5" />{isIndexedLinkedIn ? 'Provider date: ' : ''}{formatLeadDate(lead.publishedAt)}</span>
                    <span className="font-semibold">Relevance <span className="ml-1 text-slate-200">{relevance}%</span></span>
                  </div>
                  <div className="flex flex-wrap items-center justify-between gap-2 border-t border-slate-800/80 pt-4">
                    <button
                      type="button"
                      onClick={() => handleUpdateStatus(lead.id, 'SHORTLISTED', lead.userNotes)}
                      className={`inline-flex items-center gap-2 rounded-lg border px-3 py-2 text-xs font-semibold transition-colors ${
                        lead.reviewStatus === 'SHORTLISTED'
                          ? 'border-violet-400/40 bg-violet-500/15 text-violet-300'
                          : 'border-slate-700 bg-slate-900/60 text-slate-300 hover:border-violet-400/40 hover:text-violet-300'
                      }`}
                    >
                      <Bookmark className="h-3.5 w-3.5" />{lead.reviewStatus === 'SHORTLISTED' ? 'Shortlisted' : 'Shortlist'}
                    </button>
                    <div className="ml-auto flex items-center gap-2">
                      <button type="button" onClick={() => setSelectedLead(lead)} className="rounded-lg border border-slate-700 bg-slate-900/60 px-3 py-2 text-xs font-semibold text-slate-300 transition-colors hover:bg-slate-800">
                        Details
                      </button>
                      {sourceUrl && (
                        <a href={sourceUrl} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1.5 rounded-lg bg-violet-600 px-3 py-2 text-xs font-semibold text-white transition-colors hover:bg-violet-500">
                          Open source <ExternalLink className="h-3.5 w-3.5" />
                        </a>
                      )}
                      {!sourceUrl && isIndexedLinkedIn && (
                        <span className="text-[11px] text-slate-500" title="The search provider did not return a direct link to an individual LinkedIn post.">
                          Exact post link unavailable
                        </span>
                      )}
                      <button
                        type="button"
                        onClick={() => handleDeleteLead(lead)}
                        disabled={deletingLeadId === lead.id}
                        aria-label={`Delete lead: ${lead.title || 'untitled'}`}
                        title="Delete lead"
                        className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-700 bg-slate-900/60 text-slate-400 transition-colors hover:border-rose-500/40 hover:bg-rose-500/10 hover:text-rose-400 disabled:cursor-wait disabled:opacity-50"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      ) : (
        /* Table View */
        <div className="rounded-2xl glass-card border border-slate-800 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="text-slate-400 border-b border-slate-800 bg-slate-900/50">
                  <th className="py-3 px-4">Title & Requirement</th>
                  <th className="py-3 px-3">Category</th>
                  <th className="py-3 px-3">Intent</th>
                  <th className="py-3 px-3">India signal</th>
                  <th className="py-3 px-3">Platform</th>
                  <th className="py-3 px-3">Date</th>
                  <th className="py-3 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {leads.map((lead) => (
                  <tr 
                    key={lead.id}
                    onClick={() => setSelectedLead(lead)}
                    className="hover:bg-slate-800/40 cursor-pointer transition-colors"
                  >
                    <td className="py-3 px-4 font-semibold text-white max-w-md">
                      <div className="truncate">{lead.title}</div>
                      <div className="text-[11px] text-slate-400 truncate font-normal">{lead.author ? (lead.sourcePlatform === 'LINKEDIN' ? `Name in result: ${lead.author}` : lead.author) : (lead.sourcePlatform === 'LINKEDIN' ? 'Name not returned by provider' : 'Anonymous')}</div>
                    </td>
                    <td className="py-3 px-3">
                      <span className="px-2 py-0.5 rounded text-[10px] bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        {lead.serviceCategory?.replaceAll('_', ' ')}
                      </span>
                    </td>
                    <td className="py-3 px-3">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        lead.intentLevel === 'HIGH' ? 'bg-rose-500/15 text-rose-400' :
                        lead.intentLevel === 'MEDIUM' ? 'bg-amber-500/15 text-amber-300' :
                        'bg-slate-800 text-slate-400'
                      }`}>
                        {lead.intentLevel}
                      </span>
                    </td>
                    <td className="py-3 px-3">
                      {lead.isIndianMarket ? <span className="text-amber-400 font-bold">Yes</span> : <span className="text-slate-500">Global</span>}
                    </td>
                    <td className="py-3 px-3 font-mono text-slate-400">
                      {lead.sourcePlatform}
                    </td>
                    <td className="py-3 px-3 text-slate-400">
                      {lead.publishedAt ? new Date(lead.publishedAt).toLocaleDateString() : '-'}
                    </td>
                    <td className="py-3 px-4 text-right" onClick={(e) => e.stopPropagation()}>
                      <div className="inline-flex items-center space-x-1">
                        <button
                          onClick={() => handleUpdateStatus(lead.id, 'SHORTLISTED', lead.userNotes)}
                          className={`p-1 rounded ${lead.reviewStatus === 'SHORTLISTED' ? 'text-emerald-400' : 'text-slate-500 hover:text-emerald-400'}`}
                          title="Shortlist"
                        >
                          <Bookmark className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleUpdateStatus(lead.id, 'REVIEWED', lead.userNotes)}
                          className={`p-1 rounded ${lead.reviewStatus === 'REVIEWED' ? 'text-sky-400' : 'text-slate-500 hover:text-sky-400'}`}
                          title="Reviewed"
                        >
                          <CheckCircle className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleDeleteLead(lead)}
                          disabled={deletingLeadId === lead.id}
                          className="rounded p-1 text-slate-500 transition-colors hover:bg-rose-500/10 hover:text-rose-400 disabled:cursor-wait disabled:opacity-50"
                          aria-label={`Delete lead: ${lead.title || 'untitled'}`}
                          title="Delete lead"
                        >
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Pagination Controls */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-xs text-slate-400 pt-4">
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 hover:bg-slate-800 text-slate-300 disabled:opacity-40 transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="p-2 rounded-xl bg-slate-900 border border-slate-800 hover:bg-slate-800 text-slate-300 disabled:opacity-40 transition-colors"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

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
