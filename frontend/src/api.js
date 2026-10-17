import axios from 'axios';

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/+$/, '');

const api = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Leads API
export const getLeads = async (params = {}) => {
  const response = await api.get('/leads', { params });
  return response.data;
};

export const getLeadById = async (id) => {
  const response = await api.get(`/leads/${id}`);
  return response.data;
};

export const deleteLead = async (id) => {
  await api.delete(`/leads/${id}`);
};

export const deleteAllLeads = async () => {
  const response = await api.delete('/leads/all');
  return response.data;
};

export const updateLeadStatus = async (id, status, userNotes) => {
  const response = await api.patch(`/leads/${id}/status`, { status, userNotes });
  return response.data;
};

export const exportLeadsCsv = (params = {}) => {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, val]) => {
    if (val !== undefined && val !== null && val !== '') {
      searchParams.append(key, val);
    }
  });
  window.open(`${apiBaseUrl}/leads/export?${searchParams.toString()}`, '_blank', 'noopener,noreferrer');
};

// Campaigns API
export const getCampaigns = async () => {
  const response = await api.get('/campaigns');
  return response.data;
};

export const createCampaign = async (campaignData) => {
  const response = await api.post('/campaigns', campaignData);
  return response.data;
};

export const updateCampaign = async (id, campaignData) => {
  const response = await api.put(`/campaigns/${id}`, campaignData);
  return response.data;
};

export const deleteCampaign = async (id) => {
  const response = await api.delete(`/campaigns/${id}`);
  return response.data;
};

export const runCampaign = async (id) => {
  const response = await api.post(`/campaigns/${id}/run`);
  return response.data;
};

// Collection Runs API
export const getRuns = async () => {
  const response = await api.get('/collection/runs');
  return response.data;
};

export const getRunById = async (id) => {
  const response = await api.get(`/collection/runs/${id}`);
  return response.data;
};

export const triggerGlobalRun = async (campaignId = null) => {
  const response = await api.post('/collection/run', null, {
    params: campaignId ? { campaignId } : {},
  });
  return response.data;
};

// Dashboard Stats API
export const getDashboardStats = async () => {
  const response = await api.get('/dashboard/stats');
  return response.data;
};

// System Health API
export const getSystemStatus = async () => {
  const response = await api.get('/system/status');
  return response.data;
};

export default api;
