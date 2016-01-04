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

import java.net.{ InetAddress, UnknownHostException, InetSocketAddress }
import java.util.{ List => JList }
import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConversions._

import io.gatling.commons.util.JFunctions._
import io.gatling.commons.util.TimeHelper.nowMillis

import com.typesafe.scalalogging.LazyLogging
import io.netty.util.concurrent.{ ImmediateEventExecutor, Future }
import org.asynchttpclient.resolver.NameResolver
import org.xbill.DNS._
import org.xbill.DNS.Address._

import scala.util.control.NonFatal

case class DnsCacheEntry(address: InetSocketAddress, expires: Long = Long.MaxValue)

class DnsJavaNameResolver extends NameResolver with LazyLogging {

  private val hostAliases = HostsFileParser.nameToAddress

  private val cache = new ConcurrentHashMap[String, Seq[DnsCacheEntry]]()

  override def resolve(name: String, port: Int): Future[JList[InetSocketAddress]] = {

    val promise = ImmediateEventExecutor.INSTANCE.newPromise[JList[InetSocketAddress]]

    try {
      var addresses = cache.computeIfAbsent(name, (name: String) => doResolve(name, port)).collect { case entry if entry.expires > nowMillis => entry.address }
      if (addresses.isEmpty)
        addresses = cache.putIfAbsent(name, doResolve(name, port)).map(_.address)

      promise.setSuccess(addresses)

    } catch {
      case NonFatal(e) => promise.setFailure(e)
    }
  }

  private def doResolve(name: String, port: Int): Seq[DnsCacheEntry] =
    hostAliases.get(name) match {
      case Some(inetAddress) => Seq(DnsCacheEntry(new InetSocketAddress(inetAddress, port)))
      case _                 => getAddressesByName(name, port)
    }

  @throws(classOf[UnknownHostException])
  def getAddressesByName(name: String, port: Int): Seq[DnsCacheEntry] =
    toByteArray(name, IPv4) match {
      case null => toByteArray(name, IPv6) match {
        case null      => lookupHostNameAddresses(name, port)
        case ipV6Bytes => Seq(newDnsCacheEntry(name, ipV6Bytes, port))
      }
      case ipV4Bytes => Seq(newDnsCacheEntry(name, ipV4Bytes, port))
    }

  private def newDnsCacheEntry(name: String, address: Array[Byte], port: Int, ttl: Option[Long] = None): DnsCacheEntry = {
    val inetSocketAddress = new InetSocketAddress(InetAddress.getByAddress(name, address), port)
    ttl match {
      case None    => DnsCacheEntry(inetSocketAddress)
      case Some(t) => DnsCacheEntry(inetSocketAddress, nowMillis + t * 1000L)
    }
  }

  @throws(classOf[UnknownHostException])
  private def lookupHostNameAddresses(name: String, port: Int): Seq[DnsCacheEntry] =
    try {
      val lookup = new Lookup(name, Type.A)
      lookup.run match {
        case null => lookup.getResult match {
          case Lookup.TYPE_NOT_FOUND =>
            new Lookup(name, Type.AAAA).run match {
              case null => throw new UnknownHostException("unknown host")
              case rec  => rec.collect { case aaaa: AAAARecord => newDnsCacheEntry(name, aaaa.getAddress.getAddress, port, Some(aaaa.getTTL)) }
            }
          case _ => throw new UnknownHostException("unknown host")
        }

        case rec => rec.collect { case a: ARecord => newDnsCacheEntry(name, a.getAddress.getAddress, port, Some(a.getTTL)) }
      }
    } catch {
      case e: TextParseException => throw new UnknownHostException("invalid name")
    }
}
