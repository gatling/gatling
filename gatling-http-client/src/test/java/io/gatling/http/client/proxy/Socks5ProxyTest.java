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

package io.gatling.http.client.proxy;

import io.gatling.http.client.GatlingHttpClient;
import io.gatling.http.client.HttpClientConfig;
import io.gatling.http.client.Request;
import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.test.DefaultResponse;
import io.gatling.http.client.test.listener.ResponseAsStringListener;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.util.concurrent.CountDownLatch;

import static io.gatling.http.client.test.HttpTest.TIMEOUT_SECONDS;

public class Socks5ProxyTest {

  public static void main(String[] args) throws Exception {
    HttpClientConfig config = new HttpClientConfig()
      .setDefaultSslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
    try (GatlingHttpClient client = new GatlingHttpClient(config)) {

      Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create("https://gatling.io"))
        .setProxyServer(new Socks5ProxyServer("localhost", 8889, null))
        .setRequestTimeout(TIMEOUT_SECONDS * 1000)
        .build();

      final CountDownLatch latch1 = new CountDownLatch(1);
      client.execute(request, 0, true, new ResponseAsStringListener() {
        @Override
        public void onComplete() {
          System.out.println(new DefaultResponse<>(status, headers, responseBody()));
          latch1.countDown();
        }

        @Override
        public void onThrowable(Throwable e) {
          e.printStackTrace();
          latch1.countDown();
        }
      });
      latch1.await();
    }
  }
}
