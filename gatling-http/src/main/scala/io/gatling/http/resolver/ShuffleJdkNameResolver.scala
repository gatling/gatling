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

package io.gatling.http.resolver

import java.util.function.{ Function => JFunction }
import java.net.{ InetAddress, UnknownHostException }
import java.util.{ Arrays => JArrays, Collections => JCollections, List => JList }
import java.util.concurrent.{ ConcurrentHashMap, ThreadLocalRandom }

import io.netty.resolver.InetNameResolver
import io.netty.util.concurrent.{ ImmediateEventExecutor, Promise }

object ShuffleJdkNameResolver {

  private val computer: JFunction[String, JList[InetAddress]] =
    inetHost => InetAddress.getAllByName(inetHost) match {
      case Array(single) => JCollections.singletonList(single)
      case array =>
        val list = JArrays.asList(array: _*)
        JCollections.shuffle(list, ThreadLocalRandom.current)
        list
    }
}

class ShuffleJdkNameResolver extends InetNameResolver(ImmediateEventExecutor.INSTANCE) {

  private[this] val cache = new ConcurrentHashMap[String, JList[InetAddress]]

  override def doResolve(inetHost: String, promise: Promise[InetAddress]): Unit =
    throw new UnsupportedOperationException

  override def doResolveAll(inetHost: String, promise: Promise[JList[InetAddress]]): Unit =
    try {
      val addresses = cache.computeIfAbsent(inetHost, ShuffleJdkNameResolver.computer)
      promise.setSuccess(addresses)
    } catch {
      case e: UnknownHostException => promise.setFailure(e)
    }
}
