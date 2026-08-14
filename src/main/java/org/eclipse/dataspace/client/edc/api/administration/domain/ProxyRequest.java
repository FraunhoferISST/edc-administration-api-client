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

package org.eclipse.dataspace.client.edc.api.administration.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProxyRequest {

    private String httpMethod;
    private String path;
    private String queryParams;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, Object> requestBody;

    public String httpMethod() {
        return httpMethod;
    }

    public String path() {
        return path;
    }

    public String queryParams() {
        return queryParams;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public Map<String, Object> requestBody() {
        return requestBody;
    }

    public String contentType() {
        return headers.get("Content-Type");
    }

    public static class Builder {
        private ProxyRequest request;

        public Builder() {
            this.request = new ProxyRequest();
        }

        public Builder httpMethod(String httpMethod) {
            this.request.httpMethod = httpMethod;
            return this;
        }

        public Builder path(String path) {
            this.request.path = path;
            return this;
        }

        public Builder queryParams(String queryParams) {
            this.request.queryParams = queryParams;
            return this;
        }

        public Builder header(String name, String value) {
            this.request.headers.put(name, value);
            return this;
        }

        public Builder requestBody(Map<String, Object> requestBody) {
            this.request.requestBody = requestBody;
            return this;
        }

        public ProxyRequest build() {
            Objects.requireNonNull(this.request.httpMethod, "HTTP method must not be null");
            Objects.requireNonNull(this.request.path, "Path must not be null");
            return this.request;
        }
    }
}
