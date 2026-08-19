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

public interface TokenExchangeConstants {

    String GRANT_TYPE = "grant_type";
    String SUBJECT_TOKEN = "subject_token";
    String SUBJECT_TOKEN_TYPE = "subject_token_type";
    String RESOURCE = "resource";
    String SCOPE = "scope";
    String AUDIENCE = "audience";

    String GRANT_TYPE_TOKEN_EXCHANGE = "ietf:params:oauth:grant-type:token-exchange";
    String SUBJECT_TOKEN_TYPE_JWT = "urn:ietf:params:oauth:token-type:jwt";
    String TOKEN_AUDIENCE = "edcv";
}
