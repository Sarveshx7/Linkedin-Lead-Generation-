import React, { useState, useEffect } from 'react';
import { 
  History, 
  RotateCw, 
  FileText, 
  CheckCircle, 
  AlertTriangle, 
  XCircle, 
  Clock, 
  Terminal,
  X,
  Play
} from 'lucide-react';
import { getRuns, triggerGlobalRun } from '../api';

export default function CollectionHistory({ onTriggerRun, isRunning }) {
  const [runs, setRuns] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedRun, setSelectedRun] = useState(null);

  const loadRuns = async () => {
    setIsLoading(true);
    try {
      const data = await getRuns();
      setRuns(data || []);
    } catch (err) {
      console.error('Failed to load runs:', err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadRuns();
  }, []);

  const calculateDuration = (start, end) => {
    if (!start) return '-';
    const s = new Date(start).getTime();
    const e = end ? new Date(end).getTime() : Date.now();
    const sec = Math.max(0, Math.round((e - s) / 1000));
    return `${sec}s`;
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-white tracking-tight flex items-center space-x-2">
            <span>Collection History & Error Audit</span>
          </h1>
          <p className="text-xs text-slate-400">
            Audit trail of execution runs, network timing, deduplication metrics, and source diagnostics
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <button
            onClick={loadRuns}
            className="p-2 rounded-xl bg-slate-900 border border-slate-800 text-slate-400 hover:text-white transition-colors"
            title="Refresh history"
          >
            <RotateCw className="w-4 h-4" />
          </button>

          <button
            onClick={onTriggerRun}
            disabled={isRunning}
            className="px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-white text-xs font-bold transition-all shadow-md shadow-emerald-500/20 flex items-center space-x-2 disabled:opacity-50"
          >
            {isRunning ? (
              <>
                <RotateCw className="w-3.5 h-3.5 animate-spin" />
                <span>Running...</span>
              </>
            ) : (
              <>
                <Play className="w-3.5 h-3.5 fill-current" />
                <span>Trigger Run</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Runs Table */}
      {isLoading ? (
        <div className="py-20 text-center">
          <RotateCw className="w-8 h-8 text-emerald-400 animate-spin mx-auto" />
        </div>
      ) : runs.length === 0 ? (
        <div className="p-12 text-center rounded-2xl glass-card border border-slate-800 space-y-3">
          <p className="text-slate-300 font-semibold">No runs recorded yet</p>
          <button
            onClick={onTriggerRun}
            className="px-4 py-2 rounded-xl bg-emerald-500 text-white text-xs font-semibold"
          >
            Launch Initial Ingestion
          </button>
        </div>
      ) : (
        <div className="rounded-2xl glass-card border border-slate-800 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="text-slate-400 border-b border-slate-800 bg-slate-900/60">
                  <th className="py-3 px-4">Run ID</th>
                  <th className="py-3 px-3">Campaign Target</th>
                  <th className="py-3 px-3">Execution Time</th>
                  <th className="py-3 px-3">Duration</th>
                  <th className="py-3 px-3">New Leads</th>
                  <th className="py-3 px-3">Duplicates Blocked</th>
                  <th className="py-3 px-3">Sources</th>
                  <th className="py-3 px-3">Status</th>
                  <th className="py-3 px-4 text-right">Logs</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {runs.map((run) => (
                  <tr key={run.id} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-3 px-4 font-mono text-slate-400">#{run.id}</td>
                    <td className="py-3 px-3 font-semibold text-white">
                      {run.campaignName || 'Global Search'}
                    </td>
                    <td className="py-3 px-3 text-slate-300">
                      {run.startTime ? new Date(run.startTime).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' }) : '-'}
                    </td>
                    <td className="py-3 px-3 text-slate-400 font-mono">
                      {calculateDuration(run.startTime, run.endTime)}
                    </td>
                    <td className="py-3 px-3 font-mono font-bold text-emerald-400">
                      +{run.leadsDiscovered}
                    </td>
                    <td className="py-3 px-3 font-mono text-slate-400">
                      {run.duplicatesSkipped}
                    </td>
                    <td className="py-3 px-3 text-slate-300">
                      {run.sourcesAttempted} channels
                    </td>
                    <td className="py-3 px-3">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        run.status === 'COMPLETED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' :
                        run.status === 'PARTIAL_SUCCESS' ? 'bg-amber-500/10 text-amber-300 border border-amber-500/20' :
                        run.status === 'RUNNING' ? 'bg-sky-500/10 text-sky-400 border border-sky-500/20 animate-pulse' :
                        run.status === 'INTERRUPTED' ? 'bg-slate-800 text-slate-400 border border-slate-700' :
                        'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                      }`}>
                        {run.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right">
                      <button
                        onClick={() => setSelectedRun(run)}
                        className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-[11px] font-semibold border border-slate-700 transition-colors inline-flex items-center space-x-1"
                      >
                        <Terminal className="w-3 h-3 text-emerald-400" />
                        <span>Logs</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Run Diagnostics Modal */}
      {selectedRun && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="relative w-full max-w-3xl glass-panel rounded-2xl border border-slate-700 shadow-2xl p-6 sm:p-8 bg-slate-900 max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800">
              <div>
                <h2 className="text-lg font-bold text-white flex items-center space-x-2">
                  <Terminal className="w-5 h-5 text-emerald-400" />
                  <span>Run #{selectedRun.id} Execution Diagnostics</span>
                </h2>
                <p className="text-xs text-slate-400">
                  Target: {selectedRun.campaignName} | Status: {selectedRun.status}
                </p>
              </div>
              <button
                onClick={() => setSelectedRun(null)}
                className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="py-4 space-y-3 flex-1 overflow-y-auto">
              <div className="grid grid-cols-3 gap-3 p-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-center">
                <div>
                  <span className="text-slate-400 block">Discovered</span>
                  <span className="font-bold text-emerald-400 text-sm">+{selectedRun.leadsDiscovered}</span>
                </div>
                <div>
                  <span className="text-slate-400 block">Duplicates Skipped</span>
                  <span className="font-bold text-slate-300 text-sm">{selectedRun.duplicatesSkipped}</span>
                </div>
                <div>
                  <span className="text-slate-400 block">Status</span>
                  <span className="font-bold text-white text-sm">{selectedRun.status}</span>
                </div>
              </div>

              <div className="space-y-1">
                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block">
                  Log Stream & Channel Responses:
                </span>
                <pre className="p-4 rounded-xl bg-slate-950 border border-slate-800 font-mono text-xs text-emerald-300/90 whitespace-pre-wrap overflow-x-auto leading-relaxed max-h-96">
                  {selectedRun.errorLogs || 'No log messages recorded for this run.'}
                </pre>
              </div>
            </div>

            <div className="pt-3 border-t border-slate-800 flex justify-end">
              <button
                onClick={() => setSelectedRun(null)}
                className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-semibold"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
