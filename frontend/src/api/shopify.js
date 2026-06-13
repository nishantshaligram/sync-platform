import apiClient from "./client";

export const getShopifyAuthUrl = (shop) =>
  apiClient.get(`/shopify/oauth/authorize?shop=${shop}`);
