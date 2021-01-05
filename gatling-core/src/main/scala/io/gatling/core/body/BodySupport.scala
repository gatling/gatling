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

package io.gatling.core.body

import java.io.InputStream

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Expression

import com.mitchellbosecke.pebble.extension.Extension

trait BodySupport {

  def gzipBody: Body => Body = BodyProcessors.gzip
  def streamBody: Body => Body = BodyProcessors.stream

  def StringBody(string: String)(implicit configuration: GatlingConfiguration): Body with Expression[String] =
    io.gatling.core.body.ElBody(string, configuration.core.charset)

  def StringBody(string: Expression[String])(implicit configuration: GatlingConfiguration): Body with Expression[String] =
    io.gatling.core.body.StringBody(string, configuration.core.charset)

  def RawFileBody(filePath: Expression[String])(implicit rawFileBodies: RawFileBodies): Body with Expression[Array[Byte]] =
    io.gatling.core.body.RawFileBody(filePath, rawFileBodies)

  def ElFileBody(filePath: Expression[String])(implicit elFileBodies: ElFileBodies): Body with Expression[String] =
    new io.gatling.core.body.ElBody(elFileBodies.parse(filePath))

  def PebbleStringBody(string: String)(implicit configuration: GatlingConfiguration): Body with Expression[String] =
    io.gatling.core.body.PebbleStringBody(string, configuration.core.charset)

  def PebbleFileBody(
      filePath: Expression[String]
  )(implicit pebbleFileBodies: PebbleFileBodies, configuration: GatlingConfiguration): Body with Expression[String] =
    io.gatling.core.body.PebbleFileBody(filePath, pebbleFileBodies, configuration.core.charset)

  def ByteArrayBody(bytes: Expression[Array[Byte]]): Body with Expression[Array[Byte]] =
    io.gatling.core.body.ByteArrayBody(bytes)

  def InputStreamBody(is: Expression[InputStream]): Body =
    io.gatling.core.body.InputStreamBody(is)

  def registerPebbleExtensions(extensions: Extension*): Unit = PebbleExtensions.register(extensions)
}
