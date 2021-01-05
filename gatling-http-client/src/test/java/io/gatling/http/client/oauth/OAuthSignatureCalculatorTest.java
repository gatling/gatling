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
import io.gatling.http.client.RequestBuilder;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.body.form.FormUrlEncodedRequestBody;
import io.gatling.http.client.body.form.FormUrlEncodedRequestBodyBuilder;
import io.gatling.http.client.sign.OAuthSignatureCalculator;
import io.gatling.http.client.util.Utf8UrlEncoder;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the OAuth signature behavior.
 * <p>
 * See <a href= "https://oauth.googlecode.com/svn/code/javascript/example/signature.html" >Signature Tester</a> for an online oauth signature checker.
 */
class OAuthSignatureCalculatorTest {
  private static final String TOKEN_KEY = "nnch734d00sl2jdk";
  private static final String TOKEN_SECRET = "pfkkdhi9sl3r4s00";
  private static final String NONCE = "kllo9940pd9333jh";
  private static final long TIMESTAMP = 1191242096;
  private static final String CONSUMER_KEY = "dpf43f3p2l4k3l03";
  private static final String CONSUMER_SECRET = "kd94hf93k423kf44";

  // sample from RFC https://tools.ietf.org/html/rfc5849#section-3.4.1
  private void testSignatureBaseString(Request request) throws NoSuchAlgorithmException {
    String signatureBaseString = new OAuthSignatureCalculatorInstance()
      .signatureBaseString(//
        new ConsumerKey("9djdj82h48djs9d2", CONSUMER_SECRET),
        new RequestToken("kkk9d7dh3k39sjv7", TOKEN_SECRET),
        request.getMethod(),
        request.getUri(),
        request.getBody() instanceof FormUrlEncodedRequestBody ? ((FormUrlEncodedRequestBody) request.getBody()).getContent() : null,
        137131201,
        "7d8f3e4a").toString();

    assertEquals("POST&"
        + "http%3A%2F%2Fexample.com%2Frequest"
        + "&a2%3Dr%2520b%26"
        + "a3%3D2%2520q%26"
        + "a3%3Da%26"
        + "b5%3D%253D%25253D%26"
        + "c%2540%3D%26"
        + "c2%3D%26"
        + "oauth_consumer_key%3D9djdj82h48djs9d2%26"
        + "oauth_nonce%3D7d8f3e4a%26"
        + "oauth_signature_method%3DHMAC-SHA1%26"
        + "oauth_timestamp%3D137131201%26"
        + "oauth_token%3Dkkk9d7dh3k39sjv7%26"
        + "oauth_version%3D1.0",
      signatureBaseString);
  }

  // fork above test with an OAuth token that requires encoding
  private void testSignatureBaseStringWithEncodableOAuthToken(Request request) throws NoSuchAlgorithmException {
    String signatureBaseString = new OAuthSignatureCalculatorInstance()
      .signatureBaseString(//
        new ConsumerKey("9djdj82h48djs9d2", CONSUMER_SECRET),
        new RequestToken("kkk9d7dh3k39sjv7", TOKEN_SECRET),
        request.getMethod(),
        request.getUri(),
        request.getBody() instanceof FormUrlEncodedRequestBody ? ((FormUrlEncodedRequestBody) request.getBody()).getContent() : null,
        137131201,
        Utf8UrlEncoder.percentEncodeQueryElement("ZLc92RAkooZcIO/0cctl0Q==")).toString();

    assertEquals("POST&"
        + "http%3A%2F%2Fexample.com%2Frequest"
        + "&a2%3Dr%2520b%26"
        + "a3%3D2%2520q%26"
        + "a3%3Da%26"
        + "b5%3D%253D%25253D%26"
        + "c%2540%3D%26"
        + "c2%3D%26"
        + "oauth_consumer_key%3D9djdj82h48djs9d2%26"
        + "oauth_nonce%3DZLc92RAkooZcIO%252F0cctl0Q%253D%253D%26"
        + "oauth_signature_method%3DHMAC-SHA1%26"
        + "oauth_timestamp%3D137131201%26"
        + "oauth_token%3Dkkk9d7dh3k39sjv7%26"
        + "oauth_version%3D1.0",
      signatureBaseString);
  }

  @Test
  void testSignatureBaseStringWithProperlyEncodedUri() throws NoSuchAlgorithmException {

    List<Param> formParams = new ArrayList<>();
    formParams.add(new Param("c2", ""));
    formParams.add(new Param("a3", "2 q"));

    Request request = new RequestBuilder(HttpMethod.POST, Uri.create("http://example.com/request?b5=%3D%253D&a3=a&c%40=&a2=r%20b"), null)
      .setBodyBuilder(new FormUrlEncodedRequestBodyBuilder(formParams))
      .build();

    testSignatureBaseString(request);
    testSignatureBaseStringWithEncodableOAuthToken(request);
  }

  // based on the reference test case from
  // http://oauth.pbwiki.com/TestCases
  @Test
  void testGetCalculateSignature() throws NoSuchAlgorithmException, InvalidKeyException {

    Request request = new RequestBuilder(HttpMethod.GET, Uri.create("http://photos.example.net/photos?file=vacation.jpg&size=original"), null)
      .build();

    String signature = new OAuthSignatureCalculatorInstance()
      .computeSignature(new ConsumerKey(CONSUMER_KEY, CONSUMER_SECRET),
        new RequestToken(TOKEN_KEY, TOKEN_SECRET),
        request.getMethod(),
        request.getUri(),
        null,
        TIMESTAMP,
        NONCE);

    assertEquals("tR3+Ty81lMeYAr/Fid0kMTYa/WM=", signature);
  }

  @Test
  void testPostCalculateSignature() throws Exception {
    StaticOAuthSignatureCalculator calc = //
      new StaticOAuthSignatureCalculator(//
        new ConsumerKey(CONSUMER_KEY, CONSUMER_SECRET),
        new RequestToken(TOKEN_KEY, TOKEN_SECRET),
        NONCE,
        TIMESTAMP);

    List<Param> postParams = new ArrayList<>();
    postParams.add(new Param("file", "vacation.jpg"));
    postParams.add(new Param("size", "original"));

    final Request req = new RequestBuilder(HttpMethod.POST, Uri.create("http://photos.example.net/photos"), null)
      .setBodyBuilder(new FormUrlEncodedRequestBodyBuilder(postParams))
      .build();

    // From the signature tester, POST should look like:
    // normalized parameters:
    // file=vacation.jpg&oauth_consumer_key=dpf43f3p2l4k3l03&oauth_nonce=kllo9940pd9333jh&oauth_signature_method=HMAC-SHA1&oauth_timestamp=1191242096&oauth_token=nnch734d00sl2jdk&oauth_version=1.0&size=original
    // signature base string:
    // POST&http%3A%2F%2Fphotos.example.net%2Fphotos&file%3Dvacation.jpg%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_token%3Dnnch734d00sl2jdk%26oauth_version%3D1.0%26size%3Doriginal
    // signature: wPkvxykrw+BTdCcGqKr+3I+PsiM=
    // header: OAuth
    // realm="",oauth_version="1.0",oauth_consumer_key="dpf43f3p2l4k3l03",oauth_token="nnch734d00sl2jdk",oauth_timestamp="1191242096",oauth_nonce="kllo9940pd9333jh",oauth_signature_method="HMAC-SHA1",oauth_signature="wPkvxykrw%2BBTdCcGqKr%2B3I%2BPsiM%3D"

    calc.sign(req);
    String authHeader = req.getHeaders().get(AUTHORIZATION);
    Matcher m = Pattern.compile("oauth_signature=\"(.+?)\"").matcher(authHeader);
    assertTrue(m.find());
    String encodedSig = m.group(1);
    String sig = URLDecoder.decode(encodedSig, "UTF-8");

    assertEquals("wPkvxykrw+BTdCcGqKr+3I+PsiM=", sig);
  }

  @Test
  void testGetWithRequestBuilder() throws Exception {
    StaticOAuthSignatureCalculator calc =
      new StaticOAuthSignatureCalculator(
        new ConsumerKey(CONSUMER_KEY, CONSUMER_SECRET),
        new RequestToken(TOKEN_KEY, TOKEN_SECRET),
        NONCE,
        TIMESTAMP);

    final Request req = new RequestBuilder(HttpMethod.GET, Uri.create("http://photos.example.net/photos?file=vacation.jpg&size=original"), null)
      .build();

    final List<Param> params = req.getUri().getEncodedQueryParams();
    assertEquals(2, params.size());

    // From the signature tester, the URL should look like:
    // normalized parameters:
    // file=vacation.jpg&oauth_consumer_key=dpf43f3p2l4k3l03&oauth_nonce=kllo9940pd9333jh&oauth_signature_method=HMAC-SHA1&oauth_timestamp=1191242096&oauth_token=nnch734d00sl2jdk&oauth_version=1.0&size=original
    // signature base string:
    // GET&http%3A%2F%2Fphotos.example.net%2Fphotos&file%3Dvacation.jpg%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_token%3Dnnch734d00sl2jdk%26oauth_version%3D1.0%26size%3Doriginal
    // signature: tR3+Ty81lMeYAr/Fid0kMTYa/WM=
    // Authorization header: OAuth
    // realm="",oauth_version="1.0",oauth_consumer_key="dpf43f3p2l4k3l03",oauth_token="nnch734d00sl2jdk",oauth_timestamp="1191242096",oauth_nonce="kllo9940pd9333jh",oauth_signature_method="HMAC-SHA1",oauth_signature="tR3%2BTy81lMeYAr%2FFid0kMTYa%2FWM%3D"

    calc.sign(req);

    String authHeader = req.getHeaders().get(AUTHORIZATION);
    Matcher m = Pattern.compile("oauth_signature=\"(.+?)\"").matcher(authHeader);
    assertTrue(m.find());
    String encodedSig = m.group(1);
    String sig = URLDecoder.decode(encodedSig, "UTF-8");

    assertEquals("tR3+Ty81lMeYAr/Fid0kMTYa/WM=", sig);
    assertEquals("http://photos.example.net/photos?file=vacation.jpg&size=original", req.getUri().toUrl());
  }

  @Test
  void testGetWithRequestBuilderAndQuery() throws Exception {
    StaticOAuthSignatureCalculator calc = //
      new StaticOAuthSignatureCalculator(//
        new ConsumerKey(CONSUMER_KEY, CONSUMER_SECRET),
        new RequestToken(TOKEN_KEY, TOKEN_SECRET),
        NONCE,
        TIMESTAMP);

    final Request req = new RequestBuilder(HttpMethod.GET, Uri.create("http://photos.example.net/photos?file=vacation.jpg&size=original"), null)
      .build();

    final List<Param> params = req.getUri().getEncodedQueryParams();
    assertEquals(2, params.size());

    // From the signature tester, the URL should look like:
    // normalized parameters:
    // file=vacation.jpg&oauth_consumer_key=dpf43f3p2l4k3l03&oauth_nonce=kllo9940pd9333jh&oauth_signature_method=HMAC-SHA1&oauth_timestamp=1191242096&oauth_token=nnch734d00sl2jdk&oauth_version=1.0&size=original
    // signature base string:
    // GET&http%3A%2F%2Fphotos.example.net%2Fphotos&file%3Dvacation.jpg%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_token%3Dnnch734d00sl2jdk%26oauth_version%3D1.0%26size%3Doriginal
    // signature: tR3+Ty81lMeYAr/Fid0kMTYa/WM=
    // Authorization header: OAuth
    // realm="",oauth_version="1.0",oauth_consumer_key="dpf43f3p2l4k3l03",oauth_token="nnch734d00sl2jdk",oauth_timestamp="1191242096",oauth_nonce="kllo9940pd9333jh",oauth_signature_method="HMAC-SHA1",oauth_signature="tR3%2BTy81lMeYAr%2FFid0kMTYa%2FWM%3D"

    calc.sign(req);
    String authHeader = req.getHeaders().get(AUTHORIZATION);
    Matcher m = Pattern.compile("oauth_signature=\"(.+?)\"").matcher(authHeader);
    assertTrue(m.find());
    String encodedSig = m.group(1);
    String sig = URLDecoder.decode(encodedSig, "UTF-8");

    assertEquals("tR3+Ty81lMeYAr/Fid0kMTYa/WM=", sig);
    assertEquals("http://photos.example.net/photos?file=vacation.jpg&size=original", req.getUri().toUrl());
    assertEquals(

      "OAuth oauth_consumer_key=\"dpf43f3p2l4k3l03\", oauth_token=\"nnch734d00sl2jdk\", oauth_signature_method=\"HMAC-SHA1\", oauth_signature=\"tR3%2BTy81lMeYAr%2FFid0kMTYa%2FWM%3D\", oauth_timestamp=\"1191242096\", oauth_nonce=\"kllo9940pd9333jh\", oauth_version=\"1.0\"",
      authHeader);
  }

  @Test
  void testWithNullRequestToken() throws NoSuchAlgorithmException {

    final Request request = new RequestBuilder(HttpMethod.GET, Uri.create("http://photos.example.net/photos?file=vacation.jpg&size=original"), null)
      .build();

    String signatureBaseString = new OAuthSignatureCalculatorInstance()
      .signatureBaseString(//
        new ConsumerKey("9djdj82h48djs9d2", CONSUMER_SECRET),
        new RequestToken(null, null),
        request.getMethod(),
        request.getUri(),
        null,
        137131201,
        Utf8UrlEncoder.percentEncodeQueryElement("ZLc92RAkooZcIO/0cctl0Q==")).toString();

    assertEquals("GET&" +
        "http%3A%2F%2Fphotos.example.net%2Fphotos&file%3Dvacation.jpg%26" +
        "oauth_consumer_key%3D9djdj82h48djs9d2%26" +
        "oauth_nonce%3DZLc92RAkooZcIO%252F0cctl0Q%253D%253D%26" +
        "oauth_signature_method%3DHMAC-SHA1%26" +
        "oauth_timestamp%3D137131201%26" +
        "oauth_version%3D1.0%26size%3Doriginal",
      signatureBaseString);
  }

  @Test
  void testWithStarQueryParameterValue() throws NoSuchAlgorithmException {
    final Request request = new RequestBuilder(HttpMethod.GET, Uri.create("http://term.ie/oauth/example/request_token.php?testvalue=*"), null)
      .build();

    String signatureBaseString = new OAuthSignatureCalculatorInstance()
      .signatureBaseString(
        new ConsumerKey("key", "secret"),
        new RequestToken(null, null),
        request.getMethod(),
        request.getUri(),
        null,
        1469019732,
        "6ad17f97334700f3ec2df0631d5b7511").toString();

    assertEquals("GET&" +
        "http%3A%2F%2Fterm.ie%2Foauth%2Fexample%2Frequest_token.php&"
        + "oauth_consumer_key%3Dkey%26"
        + "oauth_nonce%3D6ad17f97334700f3ec2df0631d5b7511%26"
        + "oauth_signature_method%3DHMAC-SHA1%26"
        + "oauth_timestamp%3D1469019732%26"
        + "oauth_version%3D1.0%26"
        + "testvalue%3D%252A",
      signatureBaseString);
  }

  @Test
  void testSignatureGenerationWithAsteriskInPath() throws InvalidKeyException, NoSuchAlgorithmException {
    ConsumerKey consumerKey = new ConsumerKey("key", "secret");
    RequestToken requestToken = new RequestToken(null, null);
    String nonce = "6ad17f97334700f3ec2df0631d5b7511";
    long timestamp = 1469019732;

    final Request request = new RequestBuilder(HttpMethod.GET, Uri.create("http://example.com/oauth/example/*path/wi*th/asterisks*"), null)
      .build();

    String actualSignature = new OAuthSignatureCalculatorInstance().computeSignature(
      consumerKey,
      requestToken,
      request.getMethod(),
      request.getUri(),
      null,
      timestamp,
      nonce);
    assertEquals("cswi/v3ZqhVkTyy5MGqW841BxDA=", actualSignature);

    String generatedAuthHeader = new OAuthSignatureCalculatorInstance().computeAuthorizationHeader(consumerKey, requestToken, actualSignature, timestamp, nonce);
    assertTrue(generatedAuthHeader.contains("oauth_signature=\"cswi%2Fv3ZqhVkTyy5MGqW841BxDA%3D\""));
  }

  @Test
  void testPercentEncodeKeyValues() throws Exception {
    // see https://github.com/AsyncHttpClient/async-http-client/issues/1415
    String keyValue = "\u3b05\u000c\u375b";

    ConsumerKey consumer = new ConsumerKey(keyValue, "secret");
    RequestToken reqToken = new RequestToken(keyValue, "secret");
    OAuthSignatureCalculator calc = new OAuthSignatureCalculator(consumer, reqToken);

    Request request = new RequestBuilder(
      HttpMethod.GET,
      Uri.create("https://api.dropbox.com/1/oauth/access_token?oauth_token=%EC%AD%AE%E3%AC%82%EC%BE%B8%E7%9C%9A%E8%BD%BD%E1%94%A5%E8%AD%AF%E8%98%93%E0%B9%99%E5%9E%96%EF%92%A2%EA%BC%97%EA%90%B0%E4%8A%91%E8%97%BF%EF%A8%BB%E5%B5%B1%DA%98%E2%90%87%E2%96%96%EE%B5%B5%E7%B9%AD%E9%AD%87%E3%BE%93%E5%AF%92%EE%BC%8F%E3%A0%B2%E8%A9%AB%E1%8B%97%EC%BF%80%EA%8F%AE%ED%87%B0%E5%97%B7%E9%97%BF%E8%BF%87%E6%81%A3%E5%BB%A1%EC%86%92%E8%92%81%E2%B9%94%EB%B6%86%E9%AE%8A%E6%94%B0%EE%AC%B5%E6%A0%99%EB%8B%AD%EB%BA%81%E7%89%9F%E5%B3%B7%EA%9D%B7%EC%A4%9C%E0%BC%BA%EB%BB%B9%ED%84%A9%E8%A5%B9%E8%AF%A0%E3%AC%85%0C%E3%9D%9B%E8%B9%8B%E6%BF%8C%EB%91%98%E7%8B%B3%E7%BB%A8%E2%A7%BB%E6%A3%84%E1%AB%B2%E8%8D%93%E4%BF%98%E9%B9%B9%EF%9A%8B%E8%A5%93"),
      null)
      .build();

    calc.sign(request);
  }
}
