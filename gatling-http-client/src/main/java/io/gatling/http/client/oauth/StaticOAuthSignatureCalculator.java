/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.client.oauth;

import io.gatling.http.client.Param;
import io.gatling.http.client.Request;
import io.gatling.http.client.SignatureCalculator;
import io.gatling.http.client.body.form.FormUrlEncodedRequestBody;
import io.gatling.http.client.body.RequestBody;
import io.netty.handler.codec.http.HttpHeaderNames;

import java.util.Collections;
import java.util.List;

class StaticOAuthSignatureCalculator implements SignatureCalculator {

  private final ConsumerKey consumerKey;
  private final RequestToken requestToken;
  private final String nonce;
  private final long timestamp;

  StaticOAuthSignatureCalculator(ConsumerKey consumerKey, RequestToken requestToken, String nonce, long timestamp) {
    this.consumerKey = consumerKey;
    this.requestToken = requestToken;
    this.nonce = nonce;
    this.timestamp = timestamp;
  }

  @Override
  public void sign(Request request) throws Exception {

    RequestBody body = request.getBody();
    List<Param> formParams =
      body instanceof FormUrlEncodedRequestBody ?
        ((FormUrlEncodedRequestBody) body).getContent() :
        Collections.emptyList();

    String authorization = new OAuthSignatureCalculatorInstance().computeAuthorizationHeader(
      consumerKey,
      requestToken,
      request.getMethod(),
      request.getUri(),
      formParams,
      timestamp,
      nonce);

    request.getHeaders().set(HttpHeaderNames.AUTHORIZATION, authorization);
  }
}
