/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import java.net.InetAddress
import java.util.{ Collections => JCollections, List => JList }

import io.netty.resolver.NameResolver
import io.netty.util.concurrent.{ Future, ImmediateEventExecutor, Promise }

class AliasesAwareNameResolver(aliases: Map[String, InetAddress], wrapped: NameResolver[InetAddress]) extends NameResolver[InetAddress] {

  override def resolve(s: String): Future[InetAddress] =
    aliases.get(s) match {
      case Some(address) => ImmediateEventExecutor.INSTANCE.newPromise[InetAddress].setSuccess(address)
      case _             => wrapped.resolve(s)
    }

  override def resolve(s: String, promise: Promise[InetAddress]): Future[InetAddress] =
    aliases.get(s) match {
      case Some(address) => promise.setSuccess(address)
      case _             => wrapped.resolve(s, promise)
    }

  override def resolveAll(s: String): Future[JList[InetAddress]] =
    aliases.get(s) match {
      case Some(address) => ImmediateEventExecutor.INSTANCE.newPromise[JList[InetAddress]].setSuccess(JCollections.singletonList(address))
      case _             => wrapped.resolveAll(s)
    }

  override def resolveAll(s: String, promise: Promise[JList[InetAddress]]): Future[JList[InetAddress]] =
    aliases.get(s) match {
      case Some(address) => promise.setSuccess(JCollections.singletonList(address))
      case _             => wrapped.resolveAll(s, promise)
    }

  override def close(): Unit = wrapped.close()
}
