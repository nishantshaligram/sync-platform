package com.syncplatform.auth_service.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class JwtService {

    private final RSAKey rsaKey;

    public JwtService() throws Exception {
        this.rsaKey = new RSAKeyGenerator(2048)
                .keyID("auth-key-1")
                .generate();
    }

    public String generateToken(String userId, String email) throws Exception {
        JWSSigner signer = new RSASSASigner(rsaKey);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId)
                .claim("email", email)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 86400000)) // 24h
                .build();

        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claims
        );
        jwt.sign(signer);
        return jwt.serialize();
    }

    public JWKSet getPublicJwkSet() {
        return new JWKSet(rsaKey.toPublicJWK());
    }
}