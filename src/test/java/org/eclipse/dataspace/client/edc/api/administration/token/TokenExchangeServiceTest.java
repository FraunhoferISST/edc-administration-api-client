/*
 *  Copyright (c) 2026 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.dataspace.client.edc.api.administration.token;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspace.client.edc.api.administration.exception.TokenExchangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenExchangeServiceTest {

    private final TokenExchangeProperties tokenExchangeProperties = new TokenExchangeProperties();
    private final OkHttpClient okHttpClient = mock(OkHttpClient.class);
    private final Call httpCall = mock(Call.class);
    private final Response response = mock(Response.class);
    private final ResponseBody responseBody = mock(ResponseBody.class);

    private TokenExchangeService tokenExchangeService;

    @BeforeEach
    void setUp() throws Exception {
        when(okHttpClient.newCall(any())).thenReturn(httpCall);
        when(httpCall.execute()).thenReturn(response);
        when(response.body()).thenReturn(responseBody);

        tokenExchangeProperties.getServiceaccount().getToken().setMountpath("src/test/resources/mock-sa-token");
        tokenExchangeProperties.getJwtlet().setUrl("http://jwtlet");
        tokenExchangeService = new TokenExchangeService(tokenExchangeProperties, okHttpClient, new ObjectMapper());
    }

    @Test
    void exchangeToken_shouldReturnExchangedToken() throws Exception {
        var tokenExchangeResponse = "{\"access_token\": \"exchanged-token\"}";

        when(response.isSuccessful()).thenReturn(true);
        when(responseBody.string()).thenReturn(tokenExchangeResponse);

        var result = tokenExchangeService.exchangeToken(originalToken());

        assertThat(result).isEqualTo("exchanged-token");
    }

    @Test
    void exchangeToken_noParticipantContextClaimInOriginalToken_shouldThrowException() {
        var originalToken = new Jwt("tokenValue", null, null, Map.of("key", "value"), Map.of("key", "value"));

        assertThatThrownBy(() -> tokenExchangeService.exchangeToken(originalToken))
                .isInstanceOf(TokenExchangeException.class)
                .hasMessageContaining("Missing 'participantContextId' claim in JWT.");
    }

    @Test
    void exchangeToken_errorReadingSaTokenFromFile_shouldThrowException() {
        tokenExchangeProperties.getServiceaccount().getToken().setMountpath("src/test/resources/invalid-path");
        tokenExchangeService = new TokenExchangeService(tokenExchangeProperties, okHttpClient, new ObjectMapper());

        assertThatThrownBy(() -> tokenExchangeService.exchangeToken(originalToken()))
                .isInstanceOf(TokenExchangeException.class)
                .hasMessageContaining("Failed to read Service Account token.");
    }

    @Test
    void exchangeToken_jwtletNotReachable_shouldThrowException() throws Exception {
        when(httpCall.execute()).thenThrow(new IOException("Host not reachable"));

        assertThatThrownBy(() -> tokenExchangeService.exchangeToken(originalToken()))
                .isInstanceOf(TokenExchangeException.class)
                .hasMessageContaining("Failed to request token exchange from JWTlet.");
    }

    @Test
    void exchangeToken_errorResponseFromJwtlet_shouldThrowException() {
        when(response.isSuccessful()).thenReturn(false);
        when(response.code()).thenReturn(418);

        assertThatThrownBy(() -> tokenExchangeService.exchangeToken(originalToken()))
                .isInstanceOf(TokenExchangeException.class)
                .hasMessageContaining("Token exchange failed with status code: 418");
    }

    private Jwt originalToken() {
        var claims = new HashMap<String, Object>() {{
            put("participantContextId", "participantContextId");
        }};

        return new Jwt("tokenValue", null, null, Map.of("key", "value"), claims);
    }
}
