/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.gatling.http.client.Param;
import io.gatling.http.client.Request;
import io.gatling.http.client.RequestBuilder;
import io.gatling.http.client.body.form.FormUrlEncodedRequestBodyBuilder;
import io.gatling.http.client.uri.Uri;
import io.netty.handler.codec.http.HttpMethod;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the OAuth signature behavior.
 *
 * <p>See <a href= "https://oauth.googlecode.com/svn/code/javascript/example/signature.html"
 * >Signature Tester</a> for an online oauth signature checker.
 */
class OAuthSignatureCalculatorTest {
  private static final String TOKEN_KEY = "nnch734d00sl2jdk";
  private static final String TOKEN_SECRET = "pfkkdhi9sl3r4s00";
  private static final String NONCE = "kllo9940pd9333jh";
  private static final long TIMESTAMP = 1191242096;
  private static final String CONSUMER_KEY = "dpf43f3p2l4k3l03";
  private static final String CONSUMER_SECRET = "kd94hf93k423kf44";

  private static OAuthSignatureCalculator newOAuthSignatureCalculator(
      boolean useAuthorizationHeader) {
    return new OAuthSignatureCalculator(
        new ConsumerKey(CONSUMER_KEY, CONSUMER_SECRET),
        new RequestToken(TOKEN_KEY, TOKEN_SECRET),
        useAuthorizationHeader) {
      @Override
      protected OAuthSignatureCalculatorInstance getOAuthSignatureCalculatorInstance() {
        return new OAuthSignatureCalculatorInstance() {
          @Override
          protected String generateNonce() {
            return NONCE;
          }

          @Override
          protected long generateTimestamp() {
            return TIMESTAMP;
          }
        };
      }
    };
  }

  private Request getRequest() {
    final Request originalRequest =
        new RequestBuilder(
                null,
                HttpMethod.GET,
                Uri.create("http://photos.example.net/photos?file=vacation.jpg&size=original"),
                null)
            .build();

    final List<Param> params = originalRequest.getUri().getEncodedQueryParams();
    assertEquals(2, params.size());
    return originalRequest;
  }

  @Test
  void signGetRequestWithHeader() {
    Request signedRequest = newOAuthSignatureCalculator(true).apply(getRequest());

    // From the signature tester, the URL should look like:
    // normalized parameters:
    // file=vacation.jpg&oauth_consumer_key=dpf43f3p2l4k3l03&oauth_nonce=kllo9940pd9333jh&oauth_signature_method=HMAC-SHA1&oauth_timestamp=1191242096&oauth_token=nnch734d00sl2jdk&oauth_version=1.0&size=original
    // signature base string:
    // GET&http%3A%2F%2Fphotos.example.net%2Fphotos&file%3Dvacation.jpg%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_token%3Dnnch734d00sl2jdk%26oauth_version%3D1.0%26size%3Doriginal
    // signature: tR3+Ty81lMeYAr/Fid0kMTYa/WM=
    // Authorization header: OAuth
    // realm="",oauth_version="1.0",oauth_consumer_key="dpf43f3p2l4k3l03",oauth_token="nnch734d00sl2jdk",oauth_timestamp="1191242096",oauth_nonce="kllo9940pd9333jh",oauth_signature_method="HMAC-SHA1",oauth_signature="tR3%2BTy81lMeYAr%2FFid0kMTYa%2FWM%3D"
    assertEquals(
        "OAuth "
            + "oauth_consumer_key=\"dpf43f3p2l4k3l03\", "
            + "oauth_token=\"nnch734d00sl2jdk\", "
            + "oauth_signature_method=\"HMAC-SHA1\", "
            + "oauth_signature=\"tR3%2BTy81lMeYAr%2FFid0kMTYa%2FWM%3D\", "
            + "oauth_timestamp=\"1191242096\", "
            + "oauth_nonce=\"kllo9940pd9333jh\", "
            + "oauth_version=\"1.0\"",
        signedRequest.getHeaders().get(AUTHORIZATION));
    assertEquals(
        "http://photos.example.net/photos?file=vacation.jpg&size=original",
        signedRequest.getUri().toUrl());
  }

  @Test
  void signGetRequestWithQueryParam() {
    Request signedRequest = newOAuthSignatureCalculator(false).apply(getRequest());

    assertNull(signedRequest.getHeaders().get(AUTHORIZATION));
    assertEquals(
        "http://photos.example.net/photos?file=vacation.jpg&size=original"
            + "&oauth_consumer_key=dpf43f3p2l4k3l03"
            + "&oauth_token=nnch734d00sl2jdk"
            + "&oauth_signature_method=HMAC-SHA1"
            + "&oauth_signature=tR3+Ty81lMeYAr/Fid0kMTYa/WM="
            + "&oauth_timestamp=1191242096"
            + "&oauth_nonce=kllo9940pd9333jh"
            + "&oauth_version=1.0",
        signedRequest.getUri().toUrl());
  }

  private Request postRequest() {
    List<Param> postParams = new ArrayList<>();
    postParams.add(new Param("file", "vacation.jpg"));
    postParams.add(new Param("size", "original"));

    return new RequestBuilder(
            null, HttpMethod.POST, Uri.create("http://photos.example.net/photos"), null)
        .setBodyBuilder(new FormUrlEncodedRequestBodyBuilder(postParams))
        .build();
  }

  // sample from RFC https://tools.ietf.org/html/rfc5849#section-3.4.1

  @Test
  void signPostWithHeader() {
    Request signedRequest = newOAuthSignatureCalculator(true).apply(postRequest());

    // From the signature tester, POST should look like:
    // normalized parameters:
    // file=vacation.jpg&oauth_consumer_key=dpf43f3p2l4k3l03&oauth_nonce=kllo9940pd9333jh&oauth_signature_method=HMAC-SHA1&oauth_timestamp=1191242096&oauth_token=nnch734d00sl2jdk&oauth_version=1.0&size=original
    // signature base string:
    // POST&http%3A%2F%2Fphotos.example.net%2Fphotos&file%3Dvacation.jpg%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_token%3Dnnch734d00sl2jdk%26oauth_version%3D1.0%26size%3Doriginal
    // signature: wPkvxykrw+BTdCcGqKr+3I+PsiM=
    // header: OAuth
    // realm="",oauth_version="1.0",oauth_consumer_key="dpf43f3p2l4k3l03",oauth_token="nnch734d00sl2jdk",oauth_timestamp="1191242096",oauth_nonce="kllo9940pd9333jh",oauth_signature_method="HMAC-SHA1",oauth_signature="wPkvxykrw%2BBTdCcGqKr%2B3I%2BPsiM%3D"
    assertEquals(
        "OAuth "
            + "oauth_consumer_key=\"dpf43f3p2l4k3l03\", "
            + "oauth_token=\"nnch734d00sl2jdk\", "
            + "oauth_signature_method=\"HMAC-SHA1\", "
            + "oauth_signature=\"wPkvxykrw%2BBTdCcGqKr%2B3I%2BPsiM%3D\", "
            + "oauth_timestamp=\"1191242096\", "
            + "oauth_nonce=\"kllo9940pd9333jh\", "
            + "oauth_version=\"1.0\"",
        signedRequest.getHeaders().get(AUTHORIZATION));
    assertEquals("http://photos.example.net/photos", signedRequest.getUri().toUrl());
  }

  @Test
  void signPostWithFormParam() {
    Request signedRequest = newOAuthSignatureCalculator(false).apply(postRequest());

    assertNull(signedRequest.getHeaders().get(AUTHORIZATION));
    assertEquals("http://photos.example.net/photos", signedRequest.getUri().toUrl());
    String signedBody = new String(signedRequest.getBody().getBytes(), StandardCharsets.UTF_8);
    assertEquals(
        "file=vacation.jpg&size=original"
            + "&oauth_consumer_key=dpf43f3p2l4k3l03"
            + "&oauth_token=nnch734d00sl2jdk"
            + "&oauth_signature_method=HMAC-SHA1"
            + "&oauth_signature=wPkvxykrw"
            + "%2BBTdCcGqKr%2B3I%2BPsiM%3D"
            + "&oauth_timestamp=1191242096"
            + "&oauth_nonce=kllo9940pd9333jh"
            + "&oauth_version=1.0",
        signedBody);
  }
}
