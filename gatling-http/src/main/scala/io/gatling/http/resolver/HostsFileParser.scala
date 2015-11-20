/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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

import scala.io.Source
import scala.util.{ Failure, Success, Try }

import io.gatling.commons.util.{ Io, Maps }

import io.netty.util.internal.PlatformDependent
import org.xbill.DNS.Address._

object HostsFileParser {

  private def tryHostsFile: Try[Source] = {
    val path =
      if (PlatformDependent.isWindows) "C:\\Windows\\system32\\drivers\\etc\\hosts"
      else "/etc/hosts"

    Try(Source.fromFile(path))
  }

  private[resolver] def parse(source: Source): Map[String, String] = {
    val lines = Io.withSource(source)(_.getLines.toList)
    val stripped = lines.flatMap(_.split("#").headOption)
    val trimmed = stripped.map(_.trim).filterNot(_.isEmpty)
    val entries = trimmed.map(_.split("[ \t]+").toList)

    entries.collect {
      case address :: hosts if hosts.nonEmpty => hosts.map(_ -> address)
    }.reverse.flatten.toMap
  }

  private def addressToByteArray(address: String): Array[Byte] =
    toByteArray(address, IPv4) match {
      case null      => toByteArray(address, IPv6)
      case ipV4Bytes => ipV4Bytes
    }

  private[resolver] def asByteArray(addressesByHost: Map[String, String]): Map[String, Array[Byte]] = {

    import Maps._

    addressesByHost.forceMapValues(addressToByteArray).filter {
      case (_, address) => address != null
    }
  }

  def nameToAddress: Map[String, Array[Byte]] =
    tryHostsFile match {
      case Failure(_)      => Map.empty
      case Success(source) => asByteArray(parse(source))
    }
}
