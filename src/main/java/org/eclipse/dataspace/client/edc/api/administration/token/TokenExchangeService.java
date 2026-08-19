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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspace.client.edc.api.administration.exception.TokenExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.dataspace.client.edc.api.administration.token.TokenExchangeConstants.AUDIENCE;
import static org.eclipse.dataspace.client.edc.api.administration.token.TokenExchangeConstants.GRANT_TYPE;
import static org.eclipse.dataspace.client.edc.api.administration.token.TokenExchangeConstants.GRANT_TYPE_TOKEN_EXCHANGE;
import static org.eclipse.dataspace.client.edc.api.administration.token.TokenExchangeConstants.RESOURCE;
import static org.eclipse.dataspace.client.edc.api.administration.token.TokenExchangeConstants.SCOPE;
import static org.eclipse.dataspace.client.edc.api.administration.token.TokenExchangeConstants.SUBJECT_TOKEN;
import static org.eclipse.dataspace.client.edc.api.administration.token.TokenExchangeConstants.SUBJECT_TOKEN_TYPE;
import static org.eclipse.dataspace.client.edc.api.administration.token.TokenExchangeConstants.SUBJECT_TOKEN_TYPE_JWT;
import static org.eclipse.dataspace.client.edc.api.administration.token.TokenExchangeConstants.TOKEN_AUDIENCE;

@Service
public class TokenExchangeService {

    public static final String DEFAULT_SA_TOKEN_MOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    private static final String PARTICIPANT_CONTEXT_ID_CLAIM = "participantContextId";

    private final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final Logger logger = LoggerFactory.getLogger(TokenExchangeService.class);

    private final String jwtletTokenUrl;
    private final String saTokenMountPath;
    private final ResourceLoader resourceLoader;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TokenExchangeService(@Value("${tokenexchange.jwtlet.url}") String jwtletTokenUrl,
                                @Value("${tokenexchange.serviceaccount.token.mountpath:" + DEFAULT_SA_TOKEN_MOUNT_PATH + "}") String saTokenMountPath,
                                ResourceLoader resourceLoader, OkHttpClient httpClient,
                                ObjectMapper objectMapper) {
        this.jwtletTokenUrl = jwtletTokenUrl;
        this.saTokenMountPath = saTokenMountPath;
        this.resourceLoader = resourceLoader;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public String exchangeToken(Jwt jwt) throws TokenExchangeException {
        var participantContextId = jwt.getClaimAsString(PARTICIPANT_CONTEXT_ID_CLAIM);
        if (participantContextId == null) {
            var message = "Missing '%s' claim in JWT.".formatted(PARTICIPANT_CONTEXT_ID_CLAIM);
            logger.error(message);
            throw new TokenExchangeException(message);
        }

        // own SA token
        var saToken = readSaTokenFromFile();

        var tokenExchangeRequest = createTokenExchangeRequest(saToken, participantContextId);

        try (var response = httpClient.newCall(tokenExchangeRequest).execute()) {
            if (response.isSuccessful()) {
                var responseBody = response.body().string();
                var jsonResponse = objectMapper.readValue(responseBody, STRING_MAP);
                if (jsonResponse.containsKey("access_token")) {
                    return jsonResponse.get("access_token");
                }

                var message = "Missing 'access_token' in token exchange response.";
                logger.error(message);
                throw new TokenExchangeException(message);
            } else {
                var message = "Token exchange failed with status code: " + response.code();
                logger.error(message);
                throw new TokenExchangeException(message);
            }
        } catch (IOException e) {
            var message = "Failed to request token exchange from JWTlet.";
            logger.error(message, e);
            throw new TokenExchangeException(message, e);
        }
    }

    private String readSaTokenFromFile() throws TokenExchangeException {
        var file = resourceLoader.getResource(saTokenMountPath);
        try (var reader = new InputStreamReader(file.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader).trim();
        } catch (IOException e) {
            var message = "Failed to read Service Account token";
            logger.error(message, e);
            throw new TokenExchangeException(message, e);
        }
    }

    private Request createTokenExchangeRequest(String saToken, String participantContextId) {
        var formBuilder = new FormBody.Builder()
                .add(GRANT_TYPE, GRANT_TYPE_TOKEN_EXCHANGE)
                .add(SUBJECT_TOKEN, saToken)
                .add(SUBJECT_TOKEN_TYPE, SUBJECT_TOKEN_TYPE_JWT)
                .add(RESOURCE, participantContextId)
                .add(SCOPE, "admin") //TODO extract required scope from request to proxy
                .add(AUDIENCE, TOKEN_AUDIENCE);

        return new Request.Builder()
                .url(jwtletTokenUrl)
                .post(formBuilder.build())
                .build();
    }
}
