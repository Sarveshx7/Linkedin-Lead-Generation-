import React, { useState, useEffect } from 'react';
import { 
  Sliders, 
  Plus, 
  Play, 
  Edit3, 
  Trash2, 
  Check, 
  X, 
  Rss, 
  Globe, 
  AlertCircle,
  RefreshCw,
  Sparkles
} from 'lucide-react';
import { getCampaigns, createCampaign, updateCampaign, deleteCampaign, runCampaign } from '../api';

export default function CampaignManager({ onTriggerRun }) {
  const [campaigns, setCampaigns] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [editingCampaign, setEditingCampaign] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [runningId, setRunningId] = useState(null);

  // Form State
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    keywords: '',
    excludedKeywords: '',
    targetIndianMarket: true,
    enabledSources: 'LINKEDIN',
    customRssFeeds: '',
    isActive: true,
  });

  const loadCampaigns = async () => {
    setIsLoading(true);
    try {
      const data = await getCampaigns();
      setCampaigns(data || []);
    } catch (err) {
      console.error('Failed to load campaigns:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadCampaigns();
  }, []);

  const openCreateModal = () => {
    setEditingCampaign(null);
    setFormData({
      name: '',
      description: '',
      keywords: '',
      excludedKeywords: '',
      targetIndianMarket: true,
      enabledSources: 'LINKEDIN',
      customRssFeeds: '',
      isActive: true,
    });
    setIsModalOpen(true);
  };

  const openEditModal = (camp) => {
    setEditingCampaign(camp);
    setFormData({
      name: camp.name || '',
      description: camp.description || '',
      keywords: camp.keywords || '',
      excludedKeywords: camp.excludedKeywords || '',
      targetIndianMarket: true,
      enabledSources: 'LINKEDIN',
      customRssFeeds: camp.customRssFeeds || '',
      isActive: camp.isActive ?? true,
    });
    setIsModalOpen(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    const campaignData = { ...formData, targetIndianMarket: true };
    try {
      if (editingCampaign) {
        await updateCampaign(editingCampaign.id, campaignData);
      } else {
        await createCampaign(campaignData);
      }
      setIsModalOpen(false);
      loadCampaigns();
    } catch (err) {
      console.error('Failed to save campaign:', err);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this campaign?')) {
      try {
        await deleteCampaign(id);
        loadCampaigns();
      } catch (err) {
        console.error('Failed to delete campaign:', err);
      }
    }
  };

  const handleRunNow = async (id) => {
    setRunningId(id);
    try {
      await runCampaign(id);
      loadCampaigns();
      if (onTriggerRun) onTriggerRun();
    } catch (err) {
      console.error('Run failed:', err);
    } finally {
      setRunningId(null);
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center space-x-2">
            <span>Campaign & Search Manager</span>
          </h1>
          <p className="text-xs text-slate-400">
            Define target criteria, Indian market focus, and campaign keywords
          </p>
        </div>

        <button
          onClick={openCreateModal}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 hover:from-emerald-400 hover:to-teal-400 text-white text-xs font-bold transition-all shadow-md flex items-center space-x-2 self-start sm:self-auto"
        >
          <Plus className="w-4 h-4" />
          <span>New Campaign</span>
        </button>
      </div>

      {/* Campaigns List */}
      {isLoading ? (
        <div className="py-20 text-center">
          <RefreshCw className="w-8 h-8 text-emerald-400 animate-spin mx-auto" />
        </div>
      ) : campaigns.length === 0 ? (
        <div className="p-12 text-center rounded-2xl glass-card border border-slate-800 space-y-3">
          <p className="text-slate-300 font-semibold">No campaigns configured yet</p>
          <button
            onClick={openCreateModal}
            className="px-4 py-2 rounded-xl bg-emerald-500 text-white text-xs font-semibold"
          >
            Create Your First Campaign
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {campaigns.map((camp) => (
            <div
              key={camp.id}
              className="p-6 rounded-2xl glass-card border border-slate-800 hover:border-slate-700 transition-all flex flex-col justify-between space-y-4"
            >
              <div className="space-y-3">
                <div className="flex items-start justify-between">
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <h2 className="text-lg font-bold text-white">{camp.name}</h2>
                      {camp.targetIndianMarket && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/10 text-amber-300 border border-amber-500/30">
                          Indian Market
                        </span>
                      )}
                    </div>
                    <p className="text-xs text-slate-300 leading-relaxed">
                      {camp.description || 'No description provided.'}
                    </p>
                  </div>

                  <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                    camp.isActive ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-slate-800 text-slate-400'
                  }`}>
                    {camp.isActive ? 'Active' : 'Paused'}
                  </span>
                </div>

                {/* Keywords pill list */}
                <div className="space-y-1.5 pt-1">
                  <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                    Target Keywords:
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {camp.keywords.split(',').map((kw, i) => (
                      <span key={i} className="px-2 py-0.5 rounded bg-slate-950 text-slate-300 text-xs border border-slate-800">
                        {kw.trim()}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Channels */}
                <div className="pt-1 flex items-center justify-between text-xs text-slate-400">
                  <span>Search: <strong className="text-slate-300">LinkedIn via Tavily, Exa &amp; Serper</strong></span>
                  <span>Last Run: <strong className="text-slate-300">{camp.lastRunAt ? new Date(camp.lastRunAt).toLocaleTimeString() : 'Never'}</strong></span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="pt-4 border-t border-slate-800 flex items-center justify-between">
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => openEditModal(camp)}
                    className="p-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs border border-slate-700 transition-colors"
                    title="Edit Campaign"
                  >
                    <Edit3 className="w-3.5 h-3.5" />
                  </button>
                  <button
                    onClick={() => handleDelete(camp.id)}
                    className="p-2 rounded-lg bg-slate-800 hover:bg-rose-900/40 text-rose-400 text-xs border border-slate-700 transition-colors"
                    title="Delete Campaign"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>

                <button
                  onClick={() => handleRunNow(camp.id)}
                  disabled={runningId === camp.id}
                  className="px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-white text-xs font-bold transition-all flex items-center space-x-1.5 shadow-md shadow-emerald-500/20 disabled:opacity-50"
                >
                  {runningId === camp.id ? (
                    <>
                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      <span>Ingesting...</span>
                    </>
                  ) : (
                    <>
                      <Play className="w-3.5 h-3.5 fill-current" />
                      <span>Run Now</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create / Edit Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="relative w-full max-w-xl glass-panel rounded-2xl border border-slate-700 shadow-2xl p-6 sm:p-8 bg-slate-900">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <h2 className="text-lg font-bold text-white">
                {editingCampaign ? 'Edit Campaign' : 'Create New Lead Campaign'}
              </h2>
              <button 
                onClick={() => setIsModalOpen(false)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSave} className="py-4 space-y-4 text-xs">
              <div>
                <label className="block text-slate-300 font-semibold mb-1">Campaign Name</label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl p-2.5 text-slate-200 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">Description</label>
                <input
                  type="text"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl p-2.5 text-slate-200 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">Keywords (comma-separated)</label>
                <textarea
                  rows={2}
                  required
                  value={formData.keywords}
                  onChange={(e) => setFormData({ ...formData, keywords: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl p-2.5 text-slate-200 focus:outline-none focus:border-emerald-500"
                />
                <span className="text-[10px] text-slate-400 block mt-1">
                  Only these keywords are searched; there are no hidden campaign-type keywords. Results need an explicit India signal. Older posts and results without a provider date are allowed.
                </span>
              </div>

              <div>
                <label className="block text-slate-300 font-semibold mb-1">Excluded Keywords (comma-separated)</label>
                <input
                  type="text"
                  value={formData.excludedKeywords}
                  onChange={(e) => setFormData({ ...formData, excludedKeywords: e.target.value })}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl p-2.5 text-slate-200 focus:outline-none focus:border-emerald-500"
                />
              </div>

              <div className="flex items-center space-x-6 pt-2">
                <label className="flex items-center space-x-2 cursor-not-allowed">
                  <input
                    type="checkbox"
                    checked
                    disabled
                    className="rounded bg-slate-800 border-slate-700 text-amber-500 focus:ring-0"
                  />
                  <span className="font-semibold text-white">India-only targeting (required)</span>
                </label>

                <label className="flex items-center space-x-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={formData.isActive}
                    onChange={(e) => setFormData({ ...formData, isActive: e.target.checked })}
                    className="rounded bg-slate-800 border-slate-700 text-emerald-500 focus:ring-0"
                  />
                  <span className="font-semibold text-slate-300">Campaign Active</span>
                </label>
              </div>

              <div className="pt-4 border-t border-slate-800 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-white font-bold"
                >
                  Save Campaign
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
