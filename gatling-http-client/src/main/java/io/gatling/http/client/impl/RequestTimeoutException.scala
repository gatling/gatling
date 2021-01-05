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

package io.gatling.http.client.impl

import java.net.InetSocketAddress
import java.util.concurrent.TimeoutException

import io.gatling.netty.util.StringBuilderPool

object RequestTimeoutException {
  private def message(timeout: Long, remoteAddress: InetSocketAddress) = {
    val message = StringBuilderPool.DEFAULT.get.append("Request timeout")
    if (remoteAddress != null) {
      message.append(" to ").append(remoteAddress.getHostString)
      if (!remoteAddress.isUnresolved) message.append('/').append(remoteAddress.getAddress.getHostAddress)
      message.append(':').append(remoteAddress.getPort)
    }
    message.append(" after ").append(timeout).append(" ms").toString
  }
}

class RequestTimeoutException private[impl] (
    val timeout: Long,
    val remoteAddress: InetSocketAddress
) extends TimeoutException(RequestTimeoutException.message(timeout, remoteAddress)) {
  override def fillInStackTrace: Throwable = this
}
