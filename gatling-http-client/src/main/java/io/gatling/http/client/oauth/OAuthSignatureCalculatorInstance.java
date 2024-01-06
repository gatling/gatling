/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

//
// Copyright (c) 2018 AsyncHttpClient Project. All rights reserved.
//
// This program is licensed to you under the Apache License Version 2.0,
// and you may not use this file except in compliance with the Apache License Version 2.0.
// You may obtain a copy of the Apache License Version 2.0 at
//     http://www.apache.org/licenses/LICENSE-2.0.
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the Apache License Version 2.0 is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the Apache License Version 2.0 for the specific language governing permissions and
// limitations there under.
//

package io.gatling.http.client.oauth;

import static io.gatling.http.client.util.MiscUtils.isNonEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.gatling.http.client.Param;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.util.StringUtils;
import io.gatling.http.client.util.Utf8UrlEncoder;
import io.gatling.shared.util.StringBuilderPool;
import io.netty.handler.codec.http.HttpMethod;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Supports most common signature inclusion and calculation methods: HMAC-SHA1 for calculation, and
 * Header inclusion as inclusion method. Nonce generation uses simple random numbers with base64
 * encoding.
 */
class OAuthSignatureCalculatorInstance {

  public static final class Signature {

    private final ConsumerKey consumerAuth;
    private final RequestToken requestToken;
    private final long timestamp;
    private final String nonce;
    final String signature;

    public Signature(
        ConsumerKey consumerAuth,
        RequestToken requestToken,
        long timestamp,
        String nonce,
        String signature) {
      this.consumerAuth = consumerAuth;
      this.requestToken = requestToken;
      this.timestamp = timestamp;
      this.nonce = nonce;
      this.signature = signature;
    }

    String computeAuthorizationHeader() {
      StringBuilder sb = StringBuilderPool.DEFAULT.get();
      sb.append("OAuth ");
      sb.append(KEY_OAUTH_CONSUMER_KEY)
          .append("=\"")
          .append(consumerAuth.percentEncodedKey)
          .append("\", ");
      if (requestToken.key != null) {
        sb.append(KEY_OAUTH_TOKEN)
            .append("=\"")
            .append(requestToken.percentEncodedKey)
            .append("\", ");
      }
      sb.append(KEY_OAUTH_SIGNATURE_METHOD)
          .append("=\"")
          .append(OAUTH_SIGNATURE_METHOD)
          .append("\", ");

      // careful: base64 has chars that need URL encoding:
      sb.append(KEY_OAUTH_SIGNATURE).append("=\"");
      Utf8UrlEncoder.encodeAndAppendPercentEncoded(sb, signature).append("\", ");
      sb.append(KEY_OAUTH_TIMESTAMP)
          .append("=\"")
          .append(timestamp)
          .append("\", ")
          .append(KEY_OAUTH_NONCE)
          .append("=\"")
          .append(Utf8UrlEncoder.percentEncodeQueryElement(nonce))
          .append("\", ")
          .append(KEY_OAUTH_VERSION)
          .append("=\"")
          .append(OAUTH_VERSION_1_0)
          .append("\"");
      return sb.toString();
    }

    List<Param> computeParams() {
      List<Param> params = new ArrayList<>(7);
      params.add(new Param(KEY_OAUTH_CONSUMER_KEY, consumerAuth.key));
      if (requestToken.key != null) {
        params.add(new Param(KEY_OAUTH_TOKEN, requestToken.key));
      }
      params.add(new Param(KEY_OAUTH_SIGNATURE_METHOD, OAUTH_SIGNATURE_METHOD));
      params.add(new Param(KEY_OAUTH_SIGNATURE, signature));
      params.add(new Param(KEY_OAUTH_TIMESTAMP, String.valueOf(timestamp)));
      params.add(new Param(KEY_OAUTH_NONCE, nonce));
      params.add(new Param(KEY_OAUTH_VERSION, OAUTH_VERSION_1_0));
      return params;
    }
  }

  private static final String KEY_OAUTH_CONSUMER_KEY = "oauth_consumer_key";
  private static final String KEY_OAUTH_NONCE = "oauth_nonce";
  private static final String KEY_OAUTH_SIGNATURE = "oauth_signature";
  private static final String KEY_OAUTH_SIGNATURE_METHOD = "oauth_signature_method";
  private static final String KEY_OAUTH_TIMESTAMP = "oauth_timestamp";
  private static final String KEY_OAUTH_TOKEN = "oauth_token";
  private static final String KEY_OAUTH_VERSION = "oauth_version";
  private static final String OAUTH_VERSION_1_0 = "1.0";
  private static final String OAUTH_SIGNATURE_METHOD = "HMAC-SHA1";
  private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

  private final Mac mac;
  private final byte[] nonceBuffer = new byte[16];
  private final Params params = new Params();

  public OAuthSignatureCalculatorInstance() {
    try {
      mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
    } catch (NoSuchAlgorithmException e) {
      throw new UnsupportedOperationException(
          HMAC_SHA1_ALGORITHM + " is not supported, really?", e);
    }
  }

  public Signature computeSignature(
      ConsumerKey consumerAuth,
      RequestToken requestToken,
      HttpMethod method,
      Uri uri,
      List<Param> formParams) {
    String nonce = generateNonce();
    long timestamp = generateTimestamp();

    StringBuilder sb =
        signatureBaseString(consumerAuth, requestToken, method, uri, formParams, timestamp, nonce);

    ByteBuffer rawBase = StringUtils.charSequence2ByteBuffer(sb, UTF_8);
    byte[] rawSignature = digest(consumerAuth, requestToken, rawBase);
    // and finally, base64 encoded... phew!
    String signature = Base64.getEncoder().encodeToString(rawSignature);

    return new Signature(consumerAuth, requestToken, timestamp, nonce, signature);
  }

  protected String generateNonce() {
    ThreadLocalRandom.current().nextBytes(nonceBuffer);
    // let's use base64 encoding over hex, slightly more compact than hex or decimals
    return Base64.getEncoder().encodeToString(nonceBuffer);
  }

  protected long generateTimestamp() {
    return System.currentTimeMillis() / 1000L;
  }

  StringBuilder signatureBaseString(
      ConsumerKey consumerAuth,
      RequestToken requestToken,
      HttpMethod method,
      Uri uri,
      List<Param> formParams,
      long oauthTimestamp,
      String nonce) {

    // beware: must generate first as we're using pooled StringBuilder
    String baseUrl = uri.toUrlWithoutQuery();
    String encodedParams =
        encodedParams(
            consumerAuth,
            requestToken,
            oauthTimestamp,
            Utf8UrlEncoder.percentEncodeQueryElement(nonce),
            uri.getEncodedQueryParams(),
            formParams);

    StringBuilder sb = StringBuilderPool.DEFAULT.get();
    sb.append(method.name()); // POST / GET etc (nothing to URL encode)
    sb.append('&');
    Utf8UrlEncoder.encodeAndAppendPercentEncoded(sb, baseUrl);

    // and all that needs to be URL encoded (... again!)
    sb.append('&');
    Utf8UrlEncoder.encodeAndAppendPercentEncoded(sb, encodedParams);
    return sb;
  }

  private String encodedParams(
      ConsumerKey consumerAuth,
      RequestToken requestToken,
      long oauthTimestamp,
      String percentEncodedNonce,
      List<Param> queryParams,
      List<Param> formParams) {

    params.reset();

    // List of all query and form parameters added to this request; needed for calculating request
    // signature
    // Start with standard OAuth parameters we need
    params
        .add(KEY_OAUTH_CONSUMER_KEY, consumerAuth.percentEncodedKey)
        .add(KEY_OAUTH_NONCE, percentEncodedNonce)
        .add(KEY_OAUTH_SIGNATURE_METHOD, OAUTH_SIGNATURE_METHOD)
        .add(KEY_OAUTH_TIMESTAMP, String.valueOf(oauthTimestamp));
    if (requestToken.key != null) {
      params.add(KEY_OAUTH_TOKEN, requestToken.percentEncodedKey);
    }
    params.add(KEY_OAUTH_VERSION, OAUTH_VERSION_1_0);

    if (formParams != null) {
      for (Param param : formParams) {
        // formParams are not already encoded
        params.add(
            Utf8UrlEncoder.percentEncodeQueryElement(param.getName()),
            Utf8UrlEncoder.percentEncodeQueryElement(param.getValue()));
      }
    }
    if (isNonEmpty(queryParams)) {
      for (Param param : queryParams) {
        // queryParams are already form-url-encoded
        // but OAuth1 uses RFC3986_UNRESERVED_CHARS so * and + have to be encoded
        params.add(
            percentEncodeAlreadyFormUrlEncoded(param.getName()),
            percentEncodeAlreadyFormUrlEncoded(param.getValue()));
      }
    }
    return params.sortAndConcat();
  }

  private String percentEncodeAlreadyFormUrlEncoded(String s) {
    return s.replace("*", "%2A").replace("+", "%20").replace("%7E", "~");
  }

  private byte[] digest(ConsumerKey consumerAuth, RequestToken requestToken, ByteBuffer message) {
    StringBuilder sb = StringBuilderPool.DEFAULT.get();
    Utf8UrlEncoder.encodeAndAppendQueryElement(sb, consumerAuth.secret);
    sb.append('&');
    if (requestToken != null && requestToken.secret != null) {
      Utf8UrlEncoder.encodeAndAppendQueryElement(sb, requestToken.secret);
    }
    byte[] keyBytes = StringUtils.charSequence2Bytes(sb, UTF_8);
    SecretKeySpec signingKey = new SecretKeySpec(keyBytes, HMAC_SHA1_ALGORITHM);

    try {
      mac.init(signingKey);
    } catch (InvalidKeyException e) {
      throw new IllegalArgumentException("Failed to init Mac", e);
    }
    mac.update(message);
    return mac.doFinal();
  }
}
