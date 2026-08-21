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

package org.eclipse.dataspace.client.edc.api.administration.api;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.dataspace.client.edc.api.administration.domain.ProxyRequest;
import org.eclipse.dataspace.client.edc.api.administration.domain.ProxyResponse;
import org.eclipse.dataspace.client.edc.api.administration.exception.ProxyException;
import org.eclipse.dataspace.client.edc.api.administration.exception.TokenExchangeException;
import org.eclipse.dataspace.client.edc.api.administration.service.ProxyService;
import org.eclipse.dataspace.client.edc.api.administration.token.TokenExchangeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.eclipse.dataspace.client.edc.api.administration.api.ProxyApiPaths.CONTROL_PLANE_PROXY_BASE_PATH;
import static org.eclipse.dataspace.client.edc.api.administration.api.ProxyApiPaths.IDENTITY_HUB_PROXY_BASE_PATH;
import static org.eclipse.dataspace.client.edc.api.administration.api.ProxyApiPaths.ISSUER_SERVICE_PROXY_BASE_PATH;
import static org.eclipse.dataspace.client.edc.api.administration.domain.ProxyRequest.Service.CONTROL_PLANE;
import static org.eclipse.dataspace.client.edc.api.administration.domain.ProxyRequest.Service.IDENTITY_HUB;
import static org.eclipse.dataspace.client.edc.api.administration.domain.ProxyRequest.Service.ISSUER_SERVICE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.OPTIONS;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    private final TokenExchangeService tokenExchangeService;
    private final ProxyService proxyService;

    public ProxyController(TokenExchangeService tokenExchangeService, ProxyService proxyService) {
        this.tokenExchangeService = tokenExchangeService;
        this.proxyService = proxyService;
    }

    @RequestMapping(value = CONTROL_PLANE_PROXY_BASE_PATH + "/**", method = {OPTIONS, GET, POST, PUT, DELETE})
    public ResponseEntity<Map<String, Object>> proxyControlPlaneRequest(@AuthenticationPrincipal Jwt jwt,
                                                                        @RequestBody(required = false) Map<String, Object> requestBody,
                                                                        HttpServletRequest request) throws TokenExchangeException, ProxyException {
        var proxyRequest = createProxyRequest(jwt, CONTROL_PLANE, requestBody, request);

        var proxyResponse = proxyService.proxyControlPlaneRequest(proxyRequest);

        return toResponseEntity(proxyResponse);
    }

    @RequestMapping(value = IDENTITY_HUB_PROXY_BASE_PATH + "/**", method = {OPTIONS, GET, POST, PUT, DELETE})
    public ResponseEntity<Map<String, Object>> proxyIdentityHubRequest(@AuthenticationPrincipal Jwt jwt,
                                                                       @RequestBody(required = false) Map<String, Object> requestBody,
                                                                       HttpServletRequest request) throws TokenExchangeException, ProxyException {
        var proxyRequest = createProxyRequest(jwt, IDENTITY_HUB, requestBody, request);

        var proxyResponse = proxyService.proxyIdentityHubRequest(proxyRequest);

        return toResponseEntity(proxyResponse);
    }

    @RequestMapping(value = ISSUER_SERVICE_PROXY_BASE_PATH + "/**", method = {OPTIONS, GET, POST, PUT, DELETE})
    public ResponseEntity<Map<String, Object>> proxyIssuerServiceRequest(@AuthenticationPrincipal Jwt jwt,
                                                                         @RequestBody(required = false) Map<String, Object> requestBody,
                                                                         HttpServletRequest request) throws TokenExchangeException, ProxyException {
        var proxyRequest = createProxyRequest(jwt, ISSUER_SERVICE, requestBody, request);

        var proxyResponse = proxyService.proxyIssuerServiceRequest(proxyRequest);

        return toResponseEntity(proxyResponse);
    }

    private ProxyRequest createProxyRequest(Jwt jwt, ProxyRequest.Service service, Map<String, Object> requestBody, HttpServletRequest request) throws TokenExchangeException {
        var token = tokenExchangeService.exchangeToken(jwt);

        var builder = new ProxyRequest.Builder()
                .service(service)
                .token(token)
                .httpMethod(request.getMethod())
                .path(getProxyPath(request, service))
                .queryParams(request.getQueryString());

        //TODO should more headers be proxied?
        builder.header("Content-Type", request.getHeader("Content-Type"));

        if (requestBody != null) {
            builder.requestBody(requestBody);
        }

        return builder.build();
    }

    private String getProxyPath(HttpServletRequest request, ProxyRequest.Service service) {
        var fullPath = request.getRequestURI();
        var contextPath = request.getContextPath();
        if (!contextPath.isEmpty()) {
            fullPath = fullPath.substring(contextPath.length());
        }

        var endpointPrefix = switch (service) {
            case CONTROL_PLANE -> CONTROL_PLANE_PROXY_BASE_PATH;
            case IDENTITY_HUB -> IDENTITY_HUB_PROXY_BASE_PATH;
            case ISSUER_SERVICE -> ISSUER_SERVICE_PROXY_BASE_PATH;
        };

        return fullPath.substring(endpointPrefix.length());
    }

    private ResponseEntity<Map<String, Object>> toResponseEntity(ProxyResponse proxyResponse) {
        var builder = ResponseEntity.status(proxyResponse.statusCode());
        builder.body(proxyResponse.responseBody());
        proxyResponse.headers().forEach(builder::header);
        return builder.build();
    }
}
