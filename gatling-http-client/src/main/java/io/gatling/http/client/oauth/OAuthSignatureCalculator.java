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

package io.gatling.http.client.oauth;

import static io.gatling.http.client.oauth.OAuthSignatureCalculatorInstance.*;
import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;

import io.gatling.http.client.Param;
import io.gatling.http.client.Request;
import io.gatling.http.client.body.RequestBody;
import io.gatling.http.client.body.form.FormUrlEncodedRequestBody;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.uri.UriEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class OAuthSignatureCalculator implements Function<Request, Request> {

  private static final ThreadLocal<OAuthSignatureCalculatorInstance> INSTANCES =
      ThreadLocal.withInitial(OAuthSignatureCalculatorInstance::new);

  protected OAuthSignatureCalculatorInstance getOAuthSignatureCalculatorInstance() {
    return INSTANCES.get();
  }

  private final ConsumerKey consumerAuth;
  private final RequestToken requestToken;

  private final boolean useAuthorizationHeader;

  public OAuthSignatureCalculator(
      ConsumerKey consumerAuth, RequestToken requestToken, boolean useAuthorizationHeader) {
    this.consumerAuth = consumerAuth;
    this.requestToken = requestToken;
    this.useAuthorizationHeader = useAuthorizationHeader;
  }

  @Override
  public Request apply(Request request) {
    RequestBody body = request.getBody();
    List<Param> formParams =
        body instanceof FormUrlEncodedRequestBody
            ? ((FormUrlEncodedRequestBody) body).getContent()
            : Collections.emptyList();
    Signature signature =
        getOAuthSignatureCalculatorInstance()
            .computeSignature(
                consumerAuth, requestToken, request.getMethod(), request.getUri(), formParams);

    if (useAuthorizationHeader) {
      return signWithAuthorizationHeader(request, signature);
    } else {
      if (body instanceof FormUrlEncodedRequestBody) {
        return signWithFormParams(request, signature);
      } else {
        return signWithQueryParams(request, signature);
      }
    }
  }

  private static Request signWithAuthorizationHeader(Request request, Signature signature) {
    request.getHeaders().set(AUTHORIZATION, signature.computeAuthorizationHeader());
    return request;
  }

  private static Request signWithFormParams(Request request, Signature signature) {
    FormUrlEncodedRequestBody originalBody = (FormUrlEncodedRequestBody) request.getBody();
    List<Param> newParams = new ArrayList<>(originalBody.getContent());
    newParams.addAll(signature.computeParams());

    FormUrlEncodedRequestBody newBody =
        new FormUrlEncodedRequestBody(
            newParams, originalBody.getPatchedContentType(), originalBody.getCharset());
    return request.copyWithNewBody(newBody);
  }

  private static Request signWithQueryParams(Request request, Signature signature) {
    Uri newUri = UriEncoder.RAW.encode(request.getUri(), signature.computeParams());
    return request.copyWithNewUri(newUri);
  }
}
