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

package io.gatling.http.client;

import io.gatling.http.client.body.*;
import io.gatling.http.client.body.part.ByteArrayPart;
import io.gatling.http.client.body.part.FilePart;
import io.gatling.http.client.body.part.Part;
import io.gatling.http.client.body.part.StringPart;
import io.gatling.http.client.test.TestServer;
import io.gatling.http.client.test.HttpTest;
import io.gatling.http.client.test.listener.TestListener;
import io.gatling.http.client.ahc.uri.Uri;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static io.gatling.http.client.test.TestUtils.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

class BasicHttpTest extends HttpTest {

  private static TestServer server;

  @BeforeAll
  static void start() throws Throwable {
    server = new TestServer();
    server.start();
  }

  @AfterAll
  static void stop() throws Throwable {
    server.close();
  }

  private static String getTargetUrl() {
    return server.getHttpUrl() + "/foo/bar";
  }

  private File getTestFile() throws Throwable {
    return new File(Objects.requireNonNull(BasicHttpTest.class.getResource("/test.txt")).toURI());
  }

  @Test
  void testGetResponseBody() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        final String sentBody = "Hello World";

        server.enqueueResponse(response -> {
          response.setStatus(200);
          response.setContentType(TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET);
          writeResponseBody(response, sentBody);
        });

        Request request = new RequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).build(false);
        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            String contentLengthHeader = headers.get(CONTENT_LENGTH);
            assertNotNull(contentLengthHeader);
            assertEquals(sentBody.length(), Integer.parseInt(contentLengthHeader));
            assertContentTypesEquals(headers.get(CONTENT_TYPE), TEXT_HTML_CONTENT_TYPE_WITH_UTF_8_CHARSET);
            assertEquals(sentBody, responseBody());
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test
  void testGetWithHeaders() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {

        server.enqueueEcho();
        HttpHeaders h = new DefaultHttpHeaders();

        for (int i = 1; i < 5; i++) {
          h.add("Test" + i, "Test" + i);
        }

        Request request = new RequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setHeaders(h).build(false);

        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            for (int i = 1; i < 5; i++) {
              assertEquals("Test" + i, headers.get("X-Test" + i));
            }
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test
  void testPostWithHeadersAndFormParams() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        HttpHeaders h = new DefaultHttpHeaders();
        h.add(CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED);

        List<Param> formParams = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
          formParams.add(new Param("param_" + i, "value_" + i));
        }

        Request request = new RequestBuilder(HttpMethod.POST, Uri.create(getTargetUrl())).setHeaders(h).setBody(new FormUrlEncodedRequestBody(formParams)).build(false);

        server.enqueueEcho();

        client.test(request, 0, new TestListener() {

          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            for (int i = 1; i < 5; i++) {
              assertEquals(headers.get("X-param_" + i), "value_" + i);
            }
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test
  void testHeadHasEmptyBody() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        server.enqueueOk();
        Request request = new RequestBuilder(HttpMethod.HEAD, Uri.create(getTargetUrl())).build(false);
        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            assertTrue(chunks == null);
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test
  void testJettyRespondsWithChunkedTransferEncoding() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        server.enqueueEcho();
        Request request = new RequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).build(false);
        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            assertEquals(HttpHeaderValues.CHUNKED.toString(), headers.get(TRANSFER_ENCODING));
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test
  void testSendRequestWithByteArrayBody() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        server.enqueueEcho();
        RequestBody<?> byteArrayBody = new ByteArrayRequestBody("foo".getBytes());
        Request request = new RequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBody(byteArrayBody).build(false);
        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            assertEquals("foo", responseBody());
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }


  @Test
  void testSendRequestWithByteArraysBody() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        server.enqueueEcho();
        RequestBody<?> byteArraysBody = new ByteArraysRequestBody(new byte[][]{"foo".getBytes()});
        Request request = new RequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBody(byteArraysBody).build(false);
        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            assertEquals("foo", responseBody());
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test
  void testSendRequestWithFileBody() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        server.enqueueEcho();
        RequestBody<?> fileBody = new FileRequestBody(getTestFile());
        Request request = new RequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBody(fileBody).build(false);
        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            assertEquals("foobar", responseBody());
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test
  void testSendRequestWithFormUrlBody() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        server.enqueueEcho();
        List<Param> formParams = new ArrayList<>();
        formParams.add(new Param("foo", "bar"));
        RequestBody<?> formUrlEncodedBody = new FormUrlEncodedRequestBody(formParams);
        Request request = new RequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBody(formUrlEncodedBody).build(false);
        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            assertEquals("foo=bar", responseBody());
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test
  void testSendRequestWithInputStreamBody() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        server.enqueueEcho();
        RequestBody<?> inputStreamBody = new InputStreamRequestBody(new ByteArrayInputStream("foo".getBytes()));
        Request request = new RequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBody(inputStreamBody).build(false);
        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            assertEquals("foo", responseBody());
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test
  void testSendRequestWithStringBody() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        server.enqueueEcho();
        RequestBody<?> stringBody = new StringRequestBody("foo");
        Request request = new RequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBody(stringBody).build(false);
        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            assertEquals("foo", responseBody());
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test
  void testSendRequestWithMultipartFormDataBody() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        server.enqueueEcho();
        List<Part<?>> multiparts = new ArrayList<>();
        multiparts.add(new StringPart("part1", "foo", UTF_8, null, null, null, null));
        multiparts.add(new FilePart("part2", getTestFile(), UTF_8, null, null, null, null, null, null));
        multiparts.add(new ByteArrayPart("part3", "foo".getBytes(), UTF_8, null, null, null, null, null, null));
        RequestBody<?> multipartFormDataBody = new MultipartFormDataRequestBody(multiparts);
        Request request = new RequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBody(multipartFormDataBody).build(false);

        long minimalLength = getTestFile().length() + 2 * "foo".length() + 3 * 30; // file + 2 * foo + 3 * multipart boundary

        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            assertTrue(responseBody().length() > minimalLength);
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test()
  void listenererIsNotifiedOfRequestBuildExceptions() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        server.enqueueEcho();
        // Charset is mandatory and will trigger a NPE
        StringPart part = new StringPart("part1", "foo", null, null, null, null, null);
        RequestBody<?> multipartFormDataBody = new MultipartFormDataRequestBody(singletonList(part));
        Request request = new RequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBody(multipartFormDataBody).build(false);

        assertThrows(NullPointerException.class, () -> {
            try {
              client.test(request, 0, new TestListener() {
                @Override
                public void onComplete0() {
                  fail("Request should have failed");
                }
              }).get(TIMEOUT_SECONDS, SECONDS);
            } catch (ExecutionException e) {
              throw e.getCause();
            }
          }
        );
      })
    );
  }
}
