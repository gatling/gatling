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

package io.gatling.http.client.impl;

import static org.junit.jupiter.api.Assertions.*;

import io.gatling.http.client.RequestBuilder;
import io.gatling.http.client.impl.Http2ClientHarness.RecordingListener;
import io.gatling.http.client.pool.ChannelPoolKey;
import io.gatling.http.client.pool.RemoteKey;
import io.gatling.http.client.uri.Uri;
import io.netty.handler.codec.http.HttpMethod;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class Http2RetryBatchingTest {

  // like DefaultHttpClient#buildTx, every tx has its own key instance
  private static HttpTx tx(long clientId, String url) {
    Uri uri = Uri.create(url);
    RecordingListener listener = new RecordingListener();
    return new HttpTx(
        new RequestBuilder("request", HttpMethod.GET, uri, null).build(),
        listener,
        RequestTimeout.requestTimeout(0, listener),
        new ChannelPoolKey(clientId, RemoteKey.newKey(uri, null)),
        null);
  }

  @Test
  void shouldGroupTxsDroppedFromCoalescedConnectionByRemote() {
    // a coalesced connection serves several domains covered by the same certificate
    HttpTx a1 = tx(1, "https://a.gatling.io/1");
    HttpTx b1 = tx(1, "https://b.gatling.io/1");
    HttpTx a2 = tx(1, "https://a.gatling.io/2");
    HttpTx b2 = tx(1, "https://b.gatling.io/2");

    List<List<HttpTx>> groups =
        new ArrayList<>(DefaultHttpClient.groupByPoolKey(List.of(a1, b1, a2, b2)));

    assertEquals(List.of(List.of(a1, a2), List.of(b1, b2)), groups);
  }

  @Test
  void shouldNotGroupTxsFromDifferentVirtualUsers() {
    HttpTx user1 = tx(1, "https://gatling.io/");
    HttpTx user2 = tx(2, "https://gatling.io/");

    assertEquals(2, DefaultHttpClient.groupByPoolKey(List.of(user1, user2)).size());
  }

  @Test
  void channelPoolKeysShouldBeEqualForSameClientAndRemote() {
    Uri uri = Uri.create("https://gatling.io/foo");
    Uri sameRemote = Uri.create("https://gatling.io/bar");
    ChannelPoolKey key = new ChannelPoolKey(1, RemoteKey.newKey(uri, null));

    assertEquals(key, new ChannelPoolKey(1, RemoteKey.newKey(sameRemote, null)));
    assertEquals(
        key.hashCode(), new ChannelPoolKey(1, RemoteKey.newKey(sameRemote, null)).hashCode());
    assertNotEquals(key, new ChannelPoolKey(2, RemoteKey.newKey(uri, null)));
    assertNotEquals(
        key,
        new ChannelPoolKey(1, RemoteKey.newKey(Uri.create("https://other.gatling.io/"), null)));
  }
}
