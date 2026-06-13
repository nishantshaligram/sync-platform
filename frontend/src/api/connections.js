import apiClient from "./client";

export const listConnections = () => apiClient.get("/connections");

export const getConnection = (id) => apiClient.get(`/connections/${id}`);

export const createConnection = (data) => apiClient.post("/connections", data);

export const triggerManualSync = (id) =>
  apiClient.post(`/connections/${id}/sync`);

export const getSyncHistory = (id, page = 0, size = 20) =>
  apiClient.get(`/connections/${id}/sync-history?page=${page}&size=${size}`);

export const searchSyncHistory = (id, status, page = 0, size = 20) => {
  let url = `/connections/${id}/sync-history/search?page=${page}&size=${size}`;
  if (status) url += `&status=${status}`;
  return apiClient.get(url);
};
