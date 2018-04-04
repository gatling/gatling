/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.http.client.sign;

import io.gatling.http.client.Param;
import io.gatling.http.client.SignatureCalculator;
import io.gatling.http.client.ahc.oauth.ConsumerKey;
import io.gatling.http.client.ahc.oauth.OAuthSignatureCalculatorInstance;
import io.gatling.http.client.ahc.oauth.RequestToken;
import io.gatling.http.client.ahc.uri.Uri;
import io.gatling.http.client.body.FormUrlEncodedRequestBody;
import io.gatling.http.client.body.RequestBody;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;

import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;

public class OAuthSignatureCalculator implements SignatureCalculator {

  private static final ThreadLocal<OAuthSignatureCalculatorInstance> INSTANCES = ThreadLocal.withInitial(() -> {
    try {
      return new OAuthSignatureCalculatorInstance();
    } catch (NoSuchAlgorithmException e) {
      throw new ExceptionInInitializerError(e);
    }
  });

  private final ConsumerKey consumerAuth;
  private final RequestToken requestToken;

  public OAuthSignatureCalculator(ConsumerKey consumerAuth, RequestToken requestToken) {
    this.consumerAuth = consumerAuth;
    this.requestToken = requestToken;
  }

  @Override
  public void sign(HttpMethod method, Uri uri, HttpHeaders headers, RequestBody<?> body) throws Exception {

    List<Param> formParams =
      body instanceof FormUrlEncodedRequestBody ?
        ((FormUrlEncodedRequestBody) body).getContent() :
        Collections.emptyList();

    String authorization = INSTANCES.get().computeAuthorizationHeader(
      consumerAuth,
      requestToken,
      method,
      uri,
      formParams);

    headers.set(AUTHORIZATION, authorization);
  }
}
