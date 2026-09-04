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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tokenexchange")
public class TokenExchangeProperties {

    private Jwtlet jwtlet = new Jwtlet();
    private Request request = new Request();
    private String participantContextIdClaim = "participantContextId";
    private Serviceaccount serviceaccount = new Serviceaccount();

    public Jwtlet getJwtlet() {
        return jwtlet;
    }

    public void setJwtlet(Jwtlet jwtlet) {
        this.jwtlet = jwtlet;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public String getParticipantContextIdClaim() {
        return participantContextIdClaim;
    }

    public void setParticipantContextIdClaim(String participantContextIdClaim) {
        this.participantContextIdClaim = participantContextIdClaim;
    }

    public Serviceaccount getServiceaccount() {
        return serviceaccount;
    }

    public void setServiceaccount(Serviceaccount serviceaccount) {
        this.serviceaccount = serviceaccount;
    }

    public static class Jwtlet {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class Request {
        private Parameters parameters = new Parameters();
        private Values values = new Values();

        public Parameters getParameters() {
            return parameters;
        }

        public void setParameters(Parameters parameters) {
            this.parameters = parameters;
        }

        public Values getValues() {
            return values;
        }

        public void setValues(Values values) {
            this.values = values;
        }
    }

    public static class Parameters {
        private String grantType = "grant_type";
        private String subjectToken = "subject_token";
        private String subjectTokenType = "subject_token_type";
        private String resource = "resource";
        private String scope = "scope";
        private String audience = "audience";

        public String getGrantType() {
            return grantType;
        }

        public void setGrantType(String grantType) {
            this.grantType = grantType;
        }

        public String getSubjectToken() {
            return subjectToken;
        }

        public void setSubjectToken(String subjectToken) {
            this.subjectToken = subjectToken;
        }

        public String getSubjectTokenType() {
            return subjectTokenType;
        }

        public void setSubjectTokenType(String subjectTokenType) {
            this.subjectTokenType = subjectTokenType;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }
    }

    public static class Values {
        private String grantTypeTokenExchange = "urn:ietf:params:oauth:grant-type:token-exchange";
        private String subjectTokenTypeJwt = "urn:ietf:params:oauth:token-type:jwt";
        private String scope = "read,write";
        private String audience = "edcv";

        public String getGrantTypeTokenExchange() {
            return grantTypeTokenExchange;
        }

        public void setGrantTypeTokenExchange(String grantTypeTokenExchange) {
            this.grantTypeTokenExchange = grantTypeTokenExchange;
        }

        public String getSubjectTokenTypeJwt() {
            return subjectTokenTypeJwt;
        }

        public void setSubjectTokenTypeJwt(String subjectTokenTypeJwt) {
            this.subjectTokenTypeJwt = subjectTokenTypeJwt;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getAudience() {
            return audience;
        }

        public void setAudience(String audience) {
            this.audience = audience;
        }
    }

    public static class Serviceaccount {
        private Token token = new Token();

        public Token getToken() {
            return token;
        }

        public void setToken(Token token) {
            this.token = token;
        }
    }

    public static class Token {
        private String mountpath = "/var/run/secrets/kubernetes.io/serviceaccount/token";

        public String getMountpath() {
            return mountpath;
        }

        public void setMountpath(String mountpath) {
            this.mountpath = mountpath;
        }
    }
}
