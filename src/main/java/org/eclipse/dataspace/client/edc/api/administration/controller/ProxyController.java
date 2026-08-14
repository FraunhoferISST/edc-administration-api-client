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

package org.eclipse.dataspace.client.edc.api.administration.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.dataspace.client.edc.api.administration.domain.ProxyRequest;
import org.eclipse.dataspace.client.edc.api.administration.domain.ProxyResponse;
import org.eclipse.dataspace.client.edc.api.administration.service.ProxyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RestController
@RequestMapping("/proxy")
public class ProxyController {

    private final ProxyService proxyService;

    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @RequestMapping(value = "/controlplane/**", method = {GET, POST, PUT, DELETE})
    public ResponseEntity<Map<String, Object>> proxyControlPlaneRequest(@RequestBody(required = false) Map<String, Object> requestBody,
                                                                        HttpServletRequest request) {
        var proxyRequest = createProxyRequest(requestBody, request, "/controlplane");

        var proxyResponse = proxyService.proxyControlPlaneRequest(proxyRequest);

        return toResponseEntity(proxyResponse);
    }

    @RequestMapping(value = "/identityhub/**", method = {GET, POST, PUT, DELETE})
    public ResponseEntity<Map<String, Object>> proxyIdentityHubRequest(@RequestBody(required = false) Map<String, Object> requestBody,
                                                                       HttpServletRequest request) {
        var proxyRequest = createProxyRequest(requestBody, request, "/identityhub");

        var proxyResponse = proxyService.proxyIdentityHubRequest(proxyRequest);

        return toResponseEntity(proxyResponse);
    }

    @RequestMapping(value = "/issuerservice/**", method = {GET, POST, PUT, DELETE})
    public ResponseEntity<Map<String, Object>> proxyIssuerServiceRequest(@RequestBody(required = false) Map<String, Object> requestBody,
                                                                         HttpServletRequest request) {
        var proxyRequest = createProxyRequest(requestBody, request, "/issuerservice");

        var proxyResponse = proxyService.proxyIssuerServiceRequest(proxyRequest);

        return toResponseEntity(proxyResponse);
    }

    private String getProxyPath(HttpServletRequest request, String endpointPrefix) {
        var fullPath = request.getRequestURI();
        var contextPath = request.getContextPath();
        if (!contextPath.isEmpty()) {
            fullPath = fullPath.substring(contextPath.length());
        }

        return fullPath.substring(endpointPrefix.length());
    }

    private ProxyRequest createProxyRequest(Map<String, Object> requestBody, HttpServletRequest request, String basePath) {
        var builder = new ProxyRequest.Builder()
                .httpMethod(request.getMethod())
                .path(getProxyPath(request, basePath))
                .queryParams(request.getQueryString());

        //TODO should more headers be proxied?
        builder.header("Content-Type", request.getHeader("Content-Type"));

        if (requestBody != null) {
            builder.requestBody(requestBody);
        }

        return builder.build();
    }

    private ResponseEntity<Map<String, Object>> toResponseEntity(ProxyResponse proxyResponse) {
        var builder = ResponseEntity.status(proxyResponse.statusCode());
        builder.body(proxyResponse.responseBody());
        proxyResponse.headers().forEach(builder::header);
        return builder.build();
    }
}
