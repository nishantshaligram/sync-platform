package com.syncplatform.auth_service.dto;

import lombok.Data;

@Data
public class SignupRequest {
    private String email;
    private String password;
    private String fullName;
}