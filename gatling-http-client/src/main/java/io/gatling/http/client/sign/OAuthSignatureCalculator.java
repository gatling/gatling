/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;

import io.gatling.http.client.Param;
import io.gatling.http.client.Request;
import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.form.FormUrlEncodedRequestBody;
import io.gatling.http.client.oauth.ConsumerKey;
import io.gatling.http.client.oauth.OAuthSignatureCalculatorInstance;
import io.gatling.http.client.oauth.RequestToken;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class OAuthSignatureCalculator implements Function<Request, Request> {

  private static final ThreadLocal<OAuthSignatureCalculatorInstance> INSTANCES =
      ThreadLocal.withInitial(
          () -> {
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
  public Request apply(Request request) {

    RequestBody body = request.getBody();
    List<Param> formParams =
        body instanceof FormUrlEncodedRequestBody
            ? ((FormUrlEncodedRequestBody) body).getContent()
            : Collections.emptyList();

    try {
      String authorization =
          INSTANCES
              .get()
              .computeAuthorizationHeader(
                  consumerAuth, requestToken, request.getMethod(), request.getUri(), formParams);

      request.getHeaders().set(AUTHORIZATION, authorization);
      return request;

    } catch (InvalidKeyException e) {
      throw new IllegalArgumentException("Failed to compute OAuth signature", e);
    }
  }
}
