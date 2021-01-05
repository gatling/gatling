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

import io.gatling.http.client.uri.Uri;
import io.gatling.http.client.test.DefaultResponse;
import io.gatling.http.client.test.listener.ResponseAsStringListener;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class Http2Main {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(Http2Main.class);

  public static void main(String[] args) throws Exception {

    try (GatlingHttpClient client = new GatlingHttpClient(new HttpClientConfig())) {

      Request request = client.newRequestBuilder(HttpMethod.GET, Uri.create("https://www.bbc.com/pidgin"))
        .setHttp2Enabled(true)
        .setRequestTimeout(10000)
        .build();

      final CountDownLatch latch = new CountDownLatch(1);
      client.execute(request, 0, true, new ResponseAsStringListener() {
        @Override
        public void onComplete() {
          LOGGER.info(new DefaultResponse<>(status, headers, responseBody()).toString());
          latch.countDown();
        }

        @Override
        public void onThrowable(Throwable e) {
          e.printStackTrace();
          latch.countDown();
        }
      });
      latch.await();

      final CountDownLatch latch2 = new CountDownLatch(1);
      client.execute(request, 0, true, new ResponseAsStringListener() {
        @Override
        public void onComplete() {
          LOGGER.info(new DefaultResponse<>(status, headers, responseBody()).toString());
          latch2.countDown();
        }

        @Override
        public void onThrowable(Throwable e) {
          e.printStackTrace();
          latch2.countDown();
        }
      });
      latch2.await();
    }
  }
}
