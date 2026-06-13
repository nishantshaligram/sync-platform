import apiClient from "./client";

export const signup = (email, password, fullName) =>
  apiClient.post("/auth/signup", { email, password, fullName });

export const verifyEmail = (token) =>
  apiClient.get(`/auth/verify?token=${token}`);

export const login = (email, password) =>
  apiClient.post("/auth/login", { email, password });
