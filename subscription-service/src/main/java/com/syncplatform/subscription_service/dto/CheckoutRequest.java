package com.syncplatform.subscription_service.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private String planCode;
    private String successUrl;
    private String cancelUrl;
}