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

package io.gatling.http.client;

import io.gatling.http.client.body.*;
import io.gatling.http.client.body.bytearray.ByteArrayRequestBodyBuilder;
import io.gatling.http.client.body.stringchunks.StringChunksRequestBodyBuilder;
import io.gatling.http.client.body.file.FileRequestBodyBuilder;
import io.gatling.http.client.body.form.FormUrlEncodedRequestBodyBuilder;
import io.gatling.http.client.body.is.InputStreamRequestBodyBuilder;
import io.gatling.http.client.body.multipart.*;
import io.gatling.http.client.body.string.StringRequestBodyBuilder;
import io.gatling.http.client.test.TestServer;
import io.gatling.http.client.test.HttpTest;
import io.gatling.http.client.test.listener.TestListener;
import io.gatling.http.client.uri.Uri;
import io.gatling.netty.util.StringWithCachedBytes;
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static io.gatling.http.client.test.TestUtils.*;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING;
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

        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).build();
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

        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setHeaders(h).build();

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

        Request request = client.newRequestBuilder(HttpMethod.POST, Uri.create(getTargetUrl())).setHeaders(h).setBodyBuilder(new FormUrlEncodedRequestBodyBuilder(formParams)).build();

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
        Request request = client.newRequestBuilder(HttpMethod.HEAD, Uri.create(getTargetUrl())).build();
        client.test(request, 0, new TestListener() {
          @Override
          public void onComplete0() {
            assertEquals(200, status.code());
            assertNull(chunks);
          }
        }).get(TIMEOUT_SECONDS, SECONDS);
      }));
  }

  @Test
  void testJettyRespondsWithChunkedTransferEncoding() throws Throwable {
    withClient().run(client ->
      withServer(server).run(server -> {
        server.enqueueEcho();
        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).build();
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
        RequestBodyBuilder byteArrayBody = new ByteArrayRequestBodyBuilder("foo".getBytes(UTF_8), null);
        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl()))
          .setBodyBuilder(byteArrayBody).build();
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
        RequestBodyBuilder byteArraysBody = new StringChunksRequestBodyBuilder(Collections.singletonList(new StringWithCachedBytes("foo", UTF_8)));
        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBodyBuilder(byteArraysBody).build();
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
        RequestBodyBuilder fileBody = new FileRequestBodyBuilder(getTestFile());
        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBodyBuilder(fileBody).build();
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
        RequestBodyBuilder formUrlEncodedBody = new FormUrlEncodedRequestBodyBuilder(formParams);
        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBodyBuilder(formUrlEncodedBody).build();
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
        RequestBodyBuilder inputStreamBody = new InputStreamRequestBodyBuilder(new ByteArrayInputStream("foo".getBytes(UTF_8)));
        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBodyBuilder(inputStreamBody).build();
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
        RequestBodyBuilder stringBody = new StringRequestBodyBuilder("foo");
        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBodyBuilder(stringBody).build();
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
        multiparts.add(new StringPart("part1", "foo", UTF_8, null, null, null, null, null));
        multiparts.add(new FilePart("part2", getTestFile(), UTF_8, null, null, null, null, null, null));
        multiparts.add(new ByteArrayPart("part3", "foo".getBytes(), UTF_8, null, null, null, null, null, null));
        RequestBodyBuilder multipartFormDataBody = new MultipartFormDataRequestBodyBuilder(multiparts);
        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBodyBuilder(multipartFormDataBody).build();

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
        StringPart part = new StringPart("part1", "foo", null, null, null, null, null, null);
        RequestBodyBuilder multipartFormDataBody = new MultipartFormDataRequestBodyBuilder(singletonList(part));
        Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create(getTargetUrl())).setBodyBuilder(multipartFormDataBody).build();

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
