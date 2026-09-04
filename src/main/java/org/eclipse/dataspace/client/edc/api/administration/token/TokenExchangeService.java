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
import okio.Buffer;
import org.eclipse.dataspace.client.edc.api.administration.exception.TokenExchangeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class TokenExchangeService {

    private final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final Logger logger = LoggerFactory.getLogger(TokenExchangeService.class);

    private final TokenExchangeProperties tokenExchangeProperties;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TokenExchangeService(TokenExchangeProperties tokenExchangeProperties,
                                OkHttpClient httpClient,
                                ObjectMapper objectMapper) {
        this.tokenExchangeProperties = tokenExchangeProperties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public String exchangeToken(Jwt jwt) throws TokenExchangeException {
        var participantContextIdClaim = tokenExchangeProperties.getParticipantContextIdClaim();
        var participantContextId = jwt.getClaimAsString(participantContextIdClaim);
        if (participantContextId == null) {
            var message = "Missing '%s' claim in JWT.".formatted(participantContextIdClaim);
            logger.error(message);
            throw new TokenExchangeException(message);
        }

        // own SA token
        var saToken = readSaTokenFromFile();

        var tokenExchangeRequest = createTokenExchangeRequest(saToken, participantContextId);
        // try {
        //     var buffer = new Buffer();
        //     tokenExchangeRequest.newBuilder().build().body().writeTo(buffer);
        //     var requestBody = buffer.readUtf8();
        //     logger.info("BODY: {}", requestBody);
        // } catch (IOException e) {
        //     logger.error(e.getMessage());
        // }

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
        try {
            var saTokenMountPath = tokenExchangeProperties.getServiceaccount().getToken().getMountpath();
            var tokenPath = saTokenMountPath.startsWith("file:")
                    ? Path.of(URI.create(saTokenMountPath))
                    : Path.of(saTokenMountPath);

            return Files.readString(tokenPath, UTF_8).trim();
        } catch (IOException | RuntimeException e) {
            var message = "Failed to read Service Account token";
            logger.error(message, e);
            throw new TokenExchangeException(message, e);
        }
    }

    private Request createTokenExchangeRequest(String saToken, String participantContextId) {
        var parameters = tokenExchangeProperties.getRequest().getParameters();
        var values = tokenExchangeProperties.getRequest().getValues();

        var formBuilder = new FormBody.Builder()
                .add(parameters.getGrantType(), values.getGrantTypeTokenExchange())
                .add(parameters.getSubjectToken(), saToken)
                .add(parameters.getSubjectTokenType(), values.getSubjectTokenTypeJwt())
                .add(parameters.getScope(), values.getScope())
                .add(parameters.getAudience(), values.getAudience())
                .add(parameters.getResource(), participantContextId);

        return new Request.Builder()
                .url(tokenExchangeProperties.getJwtlet().getUrl())
                .post(formBuilder.build())
                .build();
    }
}
