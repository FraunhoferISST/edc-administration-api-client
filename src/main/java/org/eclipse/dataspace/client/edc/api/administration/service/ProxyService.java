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

package org.eclipse.dataspace.client.edc.api.administration.service;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.dataspace.client.edc.api.administration.domain.ProxyRequest;
import org.eclipse.dataspace.client.edc.api.administration.domain.ProxyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Service
public class ProxyService {

    private final TypeReference<Map<String, Object>> STRING_OBJECT_MAP = new TypeReference<>() {
    };

    private final String controlPlaneBaseUrl;
    private final String identityHubBaseUrl;
    private final String issuerServiceBaseUrl;

    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    public ProxyService(@Value("${controlplane.url}") String controlPlaneBaseUrl,
                        @Value("${identityhub.url}") String identityHubBaseUrl,
                        @Value("${issuerservice.url}") String issuerServiceBaseUrl,
                        ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.controlPlaneBaseUrl = controlPlaneBaseUrl;
        this.identityHubBaseUrl = identityHubBaseUrl;
        this.issuerServiceBaseUrl = issuerServiceBaseUrl;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public ProxyResponse proxyControlPlaneRequest(ProxyRequest proxyRequest) {
        return proxyRequest(proxyRequest, controlPlaneBaseUrl);
    }

    public ProxyResponse proxyIdentityHubRequest(ProxyRequest proxyRequest) {
        return proxyRequest(proxyRequest, identityHubBaseUrl);
    }

    public ProxyResponse proxyIssuerServiceRequest(ProxyRequest proxyRequest) {
        return proxyRequest(proxyRequest, issuerServiceBaseUrl);
    }

    private ProxyResponse proxyRequest(ProxyRequest proxyRequest, String baseUrl) {
        var requestBuilder = new Request.Builder()
                .url(requestUrl(baseUrl, proxyRequest.path(), proxyRequest.queryParams()));

        if (proxyRequest.requestBody() != null) {
            var body = RequestBody.create(objectMapper.writeValueAsString(proxyRequest.requestBody()), MediaType.get(proxyRequest.contentType()));
            requestBuilder.method(proxyRequest.httpMethod(), body);
        } else {
            requestBuilder.method(proxyRequest.httpMethod(), null);
        }

        try(var response = httpClient.newCall(requestBuilder.build()).execute()) {
            if (response.isSuccessful()) {
                var responseBuilder = new ProxyResponse.Builder()
                        .statusCode(response.code());

                response.headers().forEach(header -> responseBuilder.header(header.getFirst(), header.getSecond()));

                if (response.body() != null) {
                    var responseBody = response.body().string();
                    responseBuilder.responseBody(objectMapper.readValue(responseBody, STRING_OBJECT_MAP));
                }

                return responseBuilder.build();
            }

            //TODO error handling
            return null;
        } catch (IOException e) {
            //TODO error handling
            throw new RuntimeException(e);
        }
    }

    private String requestUrl(String baseUrl, String path, String queryParams) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (queryParams != null && !queryParams.startsWith("?")) {
            queryParams = "?" + queryParams;
        } else if (queryParams == null) {
            queryParams = "";
        }

        return baseUrl + path + queryParams;
    }
}
