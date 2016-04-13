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
package io.gatling.http.util

import java.net.InetAddress

import io.gatling.commons.util.TypeCaster
import io.gatling.commons.validation._
import io.gatling.http.cookie.CookieJar

import akka.actor.ActorRef
import io.netty.resolver.NameResolver
import org.asynchttpclient.AsyncHttpClient

object HttpTypeHelper {

  implicit val AhcTypeCaster = new TypeCaster[AsyncHttpClient] {
    @throws[ClassCastException]
    override def cast(value: Any): AsyncHttpClient =
      value match {
        case v: AsyncHttpClient => v
        case _                  => throw new ClassCastException(cceMessage(value, classOf[AsyncHttpClient]))
      }

    override def validate(value: Any): Validation[AsyncHttpClient] =
      value match {
        case v: AsyncHttpClient => v.success
        case _                  => cceMessage(value, classOf[AsyncHttpClient]).failure
      }
  }

  implicit val NameResolverTypeCaster = new TypeCaster[NameResolver[InetAddress]] {
    @throws[ClassCastException]
    override def cast(value: Any): NameResolver[InetAddress] =
      value match {
        case v: NameResolver[_] => v.asInstanceOf[NameResolver[InetAddress]]
        case _                  => throw new ClassCastException(cceMessage(value, classOf[NameResolver[InetAddress]]))
      }

    override def validate(value: Any): Validation[NameResolver[InetAddress]] =
      value match {
        case v: NameResolver[_] => v.asInstanceOf[NameResolver[InetAddress]].success
        case _                  => cceMessage(value, classOf[NameResolver[InetAddress]]).failure
      }
  }

  implicit val InetAddressTypeCaster = new TypeCaster[InetAddress] {
    @throws[ClassCastException]
    override def cast(value: Any): InetAddress =
      value match {
        case v: InetAddress => v
        case _              => throw new ClassCastException(cceMessage(value, classOf[InetAddress]))
      }

    override def validate(value: Any): Validation[InetAddress] =
      value match {
        case v: InetAddress => v.success
        case _              => cceMessage(value, classOf[InetAddress]).failure
      }
  }

  implicit val CookieJarTypeCaster = new TypeCaster[CookieJar] {
    @throws[ClassCastException]
    override def cast(value: Any): CookieJar =
      value match {
        case v: CookieJar => v
        case _            => throw new ClassCastException(cceMessage(value, classOf[CookieJar]))
      }

    override def validate(value: Any): Validation[CookieJar] =
      value match {
        case v: CookieJar => v.success
        case _            => cceMessage(value, classOf[CookieJar]).failure
      }
  }

  implicit val ActorRefTypeCaster = new TypeCaster[ActorRef] {
    @throws[ClassCastException]
    override def cast(value: Any): ActorRef =
      value match {
        case v: ActorRef => v
        case _           => throw new ClassCastException(cceMessage(value, classOf[ActorRef]))
      }

    override def validate(value: Any): Validation[ActorRef] =
      value match {
        case v: ActorRef => v.success
        case _           => cceMessage(value, classOf[ActorRef]).failure
      }
  }
}
