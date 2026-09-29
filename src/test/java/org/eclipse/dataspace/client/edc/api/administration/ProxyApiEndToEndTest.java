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

package org.eclipse.dataspace.client.edc.api.administration;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProxyApiEndToEndTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("edc.url.controlplane", () -> "http://localhost:" + wireMock.getPort() + "/cp");
        registry.add("edc.url.identityhub", () -> "http://localhost:" + wireMock.getPort() + "/ih");
        registry.add("edc.url.issuerservice", () -> "http://localhost:" + wireMock.getPort() + "/issuer");
        registry.add("edc.url.defaultapi", () -> "http://localhost:" + wireMock.getPort() + "/base");
        registry.add("tokenexchange.jwtlet.url", () -> "http://localhost:" + wireMock.getPort() + "/jwtlet/token");
        registry.add("tokenexchange.serviceaccount.token.mountpath", () -> "src/test/resources/mock-sa-token");
    }

    @BeforeEach
    void setUp() {
        var jwtletResponse = "{\"access_token\": \"exchanged-token\"}";
        wireMock.stubFor(post("/jwtlet/token").willReturn(okJson(jwtletResponse)));
    }

    @Test
    void proxyRequest_errorResponseFromJwtlet() throws Exception {
        wireMock.stubFor(post("/jwtlet/token").willReturn(aResponse()
            .withStatus(400)
            .withBody("error in token exchange")));

        var requestBuilder = MockMvcRequestBuilders.get("/proxy/controlplane/management/v5/participants/participant-context-1/assets")
            .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("participant_context_id", "participant-context-1")));

        mockMvc.perform(requestBuilder)
            .andExpect(status().is(500))
            .andExpect(content().string("Failed to proxy request."));
    }

    @ParameterizedTest
    @MethodSource("provideProxyArguments")
    void proxyRequest(String httpMethod, String stubPath, String requestPath, int httpResponseCode) throws Exception {
        var requestBody = "{\"name\": \"test\"}";
        var mockResponseBody = """
                {
                    "@context": [
                      "https://w3id.org/edc/connector/management/v2"
                    ],
                    "@type": "IdResponse",
                    "@id": "123",
                    "createdAt": 12345678
                }
                """;

        wireMock.stubFor(stubForMethod(httpMethod, stubPath).willReturn(aResponse()
            .withStatus(httpResponseCode)
            .withHeader("Content-Type", "application/json")
            .withBody(mockResponseBody)));

        var requestBuilder = MockMvcRequestBuilders.request(HttpMethod.valueOf(httpMethod), requestPath)
            .with(jwt().jwt(jwtBuilder -> jwtBuilder.claim("participant_context_id", "participant-context-1")));

        if (!httpMethod.equals("GET")) {
            requestBuilder
                .contentType("application/json")
                .content(requestBody);
        }

        mockMvc.perform(requestBuilder)
            .andExpect(status().is(httpResponseCode))
            .andExpect(content().json(mockResponseBody));

        wireMock.verify(postRequestedFor(urlEqualTo("/jwtlet/token"))
            .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
            .withRequestBody(containing("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Atoken-exchange"))
            .withRequestBody(containing("subject_token=ABCDEFG1234567"))
            .withRequestBody(containing("subject_token_type=urn%3Aietf%3Aparams%3Aoauth%3Atoken-type%3Ajwt"))
            .withRequestBody(containing("scope=read%2Cwrite"))
            .withRequestBody(containing("audience=edcv"))
            .withRequestBody(containing("resource=participant-context-1")));

        var expectedEdcCall = verifyForMethod(httpMethod, stubPath)
            .withHeader("Authorization", containing("Bearer exchanged-token"));
        if (!httpMethod.equals("GET")) {
            expectedEdcCall
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson(requestBody));
        }
        wireMock.verify(expectedEdcCall);
    }

    private static Stream<Arguments> provideProxyArguments() {
        return Stream.of(
            Arguments.of("GET", "/cp/management/v5/participants/participant-context-1/assets", "/proxy/controlplane/management/v5/participants/participant-context-1/assets", 200),
            Arguments.of("GET", "/cp/management/v5/participants/participant-context-1/assets", "/proxy/controlplane/management/v5/participants/participant-context-1/assets", 400),
            Arguments.of("POST", "/cp/management/v5/participants/participant-context-1/assets", "/proxy/controlplane/management/v5/participants/participant-context-1/assets", 200),
            Arguments.of("POST", "/cp/management/v5/participants/participant-context-1/assets", "/proxy/controlplane/management/v5/participants/participant-context-1/assets", 400),
            Arguments.of("PUT", "/cp/management/v5/participants/participant-context-1/assets", "/proxy/controlplane/management/v5/participants/participant-context-1/assets", 200),
            Arguments.of("PUT", "/cp/management/v5/participants/participant-context-1/assets", "/proxy/controlplane/management/v5/participants/participant-context-1/assets", 400),
            Arguments.of("DELETE", "/cp/management/v5/participants/participant-context-1/assets", "/proxy/controlplane/management/v5/participants/participant-context-1/assets", 200),
            Arguments.of("DELETE", "/cp/management/v5/participants/participant-context-1/assets", "/proxy/controlplane/management/v5/participants/participant-context-1/assets", 400),
            Arguments.of("GET", "/ih/participants/participant-context-1/credentials", "/proxy/identityhub/participants/participant-context-1/credentials", 200),
            Arguments.of("GET", "/ih/participants/participant-context-1/credentials", "/proxy/identityhub/participants/participant-context-1/credentials", 400),
            Arguments.of("POST", "/ih/participants/participant-context-1/credentials", "/proxy/identityhub/participants/participant-context-1/credentials", 200),
            Arguments.of("POST", "/ih/participants/participant-context-1/credentials", "/proxy/identityhub/participants/participant-context-1/credentials", 400),
            Arguments.of("PUT", "/ih/participants/participant-context-1/credentials", "/proxy/identityhub/participants/participant-context-1/credentials", 200),
            Arguments.of("PUT", "/ih/participants/participant-context-1/credentials", "/proxy/identityhub/participants/participant-context-1/credentials", 400),
            Arguments.of("DELETE", "/ih/participants/participant-context-1/credentials", "/proxy/identityhub/participants/participant-context-1/credentials", 200),
            Arguments.of("DELETE", "/ih/participants/participant-context-1/credentials", "/proxy/identityhub/participants/participant-context-1/credentials", 400),
            Arguments.of("GET", "/issuer/participants/participant-context-1/holders", "/proxy/issuerservice/participants/participant-context-1/holders", 200),
            Arguments.of("GET", "/issuer/participants/participant-context-1/holders", "/proxy/issuerservice/participants/participant-context-1/holders", 400),
            Arguments.of("POST", "/issuer/participants/participant-context-1/holders", "/proxy/issuerservice/participants/participant-context-1/holders", 200),
            Arguments.of("POST", "/issuer/participants/participant-context-1/holders", "/proxy/issuerservice/participants/participant-context-1/holders", 400),
            Arguments.of("PUT", "/issuer/participants/participant-context-1/holders", "/proxy/issuerservice/participants/participant-context-1/holders", 200),
            Arguments.of("PUT", "/issuer/participants/participant-context-1/holders", "/proxy/issuerservice/participants/participant-context-1/holders", 400),
            Arguments.of("DELETE", "/issuer/participants/participant-context-1/holders", "/proxy/issuerservice/participants/participant-context-1/holders", 200),
            Arguments.of("DELETE", "/issuer/participants/participant-context-1/holders", "/proxy/issuerservice/participants/participant-context-1/holders", 400),
            Arguments.of("GET", "/base/health", "/proxy/default/health", 200),
            Arguments.of("GET", "/base/health", "/proxy/default/health", 400)
        );
    }

    private static MappingBuilder stubForMethod(String method, String path) {
        return switch (method) {
            case "GET" -> get(urlEqualTo(path));
            case "POST" -> post(urlEqualTo(path));
            case "PUT" -> put(urlEqualTo(path));
            case "DELETE" -> delete(urlEqualTo(path));
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
    }

    private static RequestPatternBuilder verifyForMethod(String method, String path) {
        return switch (method) {
            case "GET" -> getRequestedFor(urlEqualTo(path));
            case "POST" -> postRequestedFor(urlEqualTo(path));
            case "PUT" -> putRequestedFor(urlEqualTo(path));
            case "DELETE" -> deleteRequestedFor(urlEqualTo(path));
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
    }
}
