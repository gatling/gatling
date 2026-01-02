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

import io.gatling.shared.util.StringBuilderPool;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

public final class RequestTimeoutException extends TimeoutException {

  private static String message(long timeout, InetSocketAddress remoteAddress) {
    StringBuilder message = StringBuilderPool.DEFAULT.get().append("Request timeout");
    if (remoteAddress != null) {
      message.append(" to ").append(remoteAddress.getHostString());
      if (!remoteAddress.isUnresolved()) {
        message.append('/').append(remoteAddress.getAddress().getHostAddress());
      }
      message.append(':').append(remoteAddress.getPort());
    }
    return message.append(" after ").append(timeout).append(" ms").toString();
  }

  RequestTimeoutException(long timeout, InetSocketAddress remoteAddress) {
    super(message(timeout, remoteAddress));
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
