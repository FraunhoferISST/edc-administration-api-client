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

public class ProxyResponse {

    private int statusCode;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, Object> responseBody;

    public int statusCode() {
        return statusCode;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public Map<String, Object> responseBody() {
        return responseBody;
    }

    public static class Builder {
        private ProxyResponse response;

        public Builder() {
            this.response = new ProxyResponse();
        }

        public Builder statusCode(int statusCode) {
            this.response.statusCode = statusCode;
            return this;
        }

        public Builder header(String name, String value) {
            this.response.headers.put(name, value);
            return this;
        }

        public Builder responseBody(Map<String, Object> responseBody) {
            this.response.responseBody = responseBody;
            return this;
        }

        public ProxyResponse build() {
            if (this.response.statusCode == 0) {
                throw new IllegalStateException("Response status code must be set");
            }
            return this.response;
        }
    }
}
