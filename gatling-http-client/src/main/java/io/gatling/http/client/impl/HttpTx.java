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

package io.gatling.http.client.impl;

import io.gatling.http.client.HttpListener;
import io.gatling.http.client.Request;
import io.gatling.http.client.pool.ChannelPoolKey;

public class HttpTx {

  final Request request;
  final HttpListener listener;
  final RequestTimeout requestTimeout;
  final ChannelPoolKey key;

  // mutable state
  boolean usingPooledChannel;
  int remainingTries;
  boolean closeConnection;

  HttpTx(Request request, HttpListener listener, RequestTimeout requestTimeout, ChannelPoolKey key, int remainingTries) {
    this.request = request;
    this.listener = listener;
    this.requestTimeout = requestTimeout;
    this.key = key;
    this.remainingTries = remainingTries;
  }
}
