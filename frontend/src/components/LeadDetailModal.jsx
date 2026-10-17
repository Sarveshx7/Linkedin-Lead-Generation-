import React from 'react';
import { getLeadSourceUrl } from '../utils/sourceLinks';
import { 
  X, 
  ExternalLink, 
  Calendar, 
  User, 
  Building, 
  MapPin, 
  Flame, 
  CheckCircle, 
  Bookmark, 
  XCircle
} from 'lucide-react';

export default function LeadDetailModal({ lead, onClose, onUpdateStatus }) {
  if (!lead) return null;

  const isIndexedLinkedIn = lead.sourcePlatform === 'LINKEDIN';
  const authorName = lead.author?.trim() || '';
  const contactInfo = lead.contactInfo?.trim() || '';
  const sourceUrl = getLeadSourceUrl(lead);

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div 
        className="relative w-full max-w-3xl glass-panel rounded-2xl border border-slate-700/80 shadow-2xl p-6 sm:p-8 bg-slate-900/95 max-h-[90vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between pb-5 border-b border-slate-800">
          <div className="space-y-2 pr-6">
            <div className="flex flex-wrap items-center gap-2">
              {lead.serviceCategory && <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                {lead.serviceCategory?.replaceAll('_', ' ')}
              </span>}

              {lead.intentLevel === 'HIGH' && (
                <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-500/15 text-rose-400 border border-rose-500/30 flex items-center gap-1">
                  <Flame className="w-3.5 h-3.5" /> High Intent
                </span>
              )}

              {lead.intentLevel === 'MEDIUM' && (
                <span className="px-2.5 py-0.5 rounded-full text-xs font-medium bg-amber-500/15 text-amber-300 border border-amber-500/30">
                  Medium Intent
                </span>
              )}

              {lead.isIndianMarket && (
                <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-amber-500/10 text-amber-400 border border-amber-500/30">
                  India market match
                </span>
              )}

              {lead.sourcePlatform && <span className="px-2.5 py-0.5 rounded-full text-xs font-mono text-slate-400 bg-slate-800 border border-slate-700">
                {lead.sourcePlatform}
              </span>}
              {lead.searchProviders && (
                <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold text-sky-300 bg-sky-500/10 border border-sky-500/20">
                  Providers: {lead.searchProviders}
                </span>
              )}
            </div>

            {lead.title && <h2 className="text-xl font-bold text-white leading-snug">
              {lead.title}
            </h2>}
          </div>

          <button 
            onClick={onClose}
            className="p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="overflow-y-auto py-5 space-y-6 flex-1 pr-1">
          {/* Fields present in the collected record */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 text-xs">
              <div>
                <span className="text-slate-400 block mb-1">Name</span>
                <div className="flex items-center space-x-1 font-semibold text-slate-200 truncate">
                  <User className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                  <span className="truncate">{authorName || 'No name found'}</span>
                </div>
              </div>
              {lead.company && <div>
                <span className="text-slate-400 block mb-1">Organization</span>
                <div className="flex items-center space-x-1 font-semibold text-slate-200 truncate">
                  <Building className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                  <span className="truncate">{lead.company}</span>
                </div>
              </div>}
              {lead.location && <div>
                <span className="text-slate-400 block mb-1">Location</span>
                <div className="flex items-center space-x-1 font-semibold text-slate-200 truncate">
                  <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                  <span className="truncate">{lead.location}</span>
                </div>
              </div>}
              {lead.publishedAt && <div>
                <span className="text-slate-400 block mb-1">{isIndexedLinkedIn ? 'Provider date' : 'Published'}</span>
                <div className="flex items-center space-x-1 font-semibold text-slate-200 truncate">
                  <Calendar className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                  <span>{new Date(lead.publishedAt).toLocaleDateString()}</span>
                </div>
              </div>}
          </div>

          <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80">
            <span className="text-xs font-semibold text-slate-400 block mb-1">Contact information</span>
            <p className="text-sm font-medium text-slate-200">{contactInfo || 'No contact info found'}</p>
          </div>

          {lead.content && (
            <div className="space-y-2">
              <h3 className="text-sm font-bold text-slate-300 uppercase tracking-wider">
                {isIndexedLinkedIn ? 'Search result text' : 'Collected content'}
              </h3>
              <div className="p-4 rounded-xl bg-slate-950 border border-slate-800 text-slate-300 text-sm leading-relaxed whitespace-pre-wrap font-sans select-text">
                {lead.content}
              </div>
            </div>
          )}

          {sourceUrl ? (
            <a
              href={sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 self-start px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-white text-xs font-semibold transition-colors"
            >
              <span>Open Post Link</span>
              <ExternalLink className="w-3.5 h-3.5" />
            </a>
          ) : isIndexedLinkedIn ? (
            <p className="inline-flex items-center rounded-xl border border-slate-700 bg-slate-900/70 px-4 py-2 text-xs text-slate-400">
              No exact LinkedIn post permalink was returned for this result.
            </p>
          ) : null}
        </div>

        {/* Footer Actions */}
        <div className="pt-4 border-t border-slate-800 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center space-x-2">
            <span className="text-xs text-slate-400">Mark Lead As:</span>
            <button
              onClick={() => onUpdateStatus(lead.id, 'SHORTLISTED', lead.userNotes)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 border transition-all ${
                lead.reviewStatus === 'SHORTLISTED'
                  ? 'bg-emerald-500 text-white border-emerald-400 shadow-lg shadow-emerald-500/20'
                  : 'bg-slate-800 hover:bg-slate-700 text-emerald-400 border-slate-700'
              }`}
            >
              <Bookmark className="w-3.5 h-3.5" />
              <span>Shortlist</span>
            </button>

            <button
              onClick={() => onUpdateStatus(lead.id, 'REVIEWED', lead.userNotes)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 border transition-all ${
                lead.reviewStatus === 'REVIEWED'
                  ? 'bg-sky-500 text-white border-sky-400'
                  : 'bg-slate-800 hover:bg-slate-700 text-sky-400 border-slate-700'
              }`}
            >
              <CheckCircle className="w-3.5 h-3.5" />
              <span>Reviewed</span>
            </button>

            <button
              onClick={() => onUpdateStatus(lead.id, 'REJECTED', lead.userNotes)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center space-x-1.5 border transition-all ${
                lead.reviewStatus === 'REJECTED'
                  ? 'bg-rose-500 text-white border-rose-400'
                  : 'bg-slate-800 hover:bg-slate-700 text-rose-400 border-slate-700'
              }`}
            >
              <XCircle className="w-3.5 h-3.5" />
              <span>Reject</span>
            </button>
          </div>

          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
