import apiClient from "./client";

export const getQboAuthUrl = () => apiClient.get("/qbo/oauth/authorize");
