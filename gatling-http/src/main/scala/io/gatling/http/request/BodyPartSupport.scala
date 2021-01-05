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

package io.gatling.http.request

import io.gatling.core.body.{ ElFileBodies, PebbleFileBodies, RawFileBodies }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session.Expression

trait BodyPartSupport {

  def ElFileBodyPart(filePath: Expression[String])(implicit configuration: GatlingConfiguration, elFileBodies: ElFileBodies): BodyPart =
    BodyPart.elFileBodyPart(None, filePath, configuration.core.charset, elFileBodies)
  def ElFileBodyPart(
      name: Expression[String],
      filePath: Expression[String]
  )(implicit configuration: GatlingConfiguration, elFileBodies: ElFileBodies): BodyPart =
    BodyPart.elFileBodyPart(Some(name), filePath, configuration.core.charset, elFileBodies)

  def StringBodyPart(string: Expression[String])(implicit configuration: GatlingConfiguration): BodyPart =
    BodyPart.stringBodyPart(None, string, configuration.core.charset)
  def StringBodyPart(name: Expression[String], string: Expression[String])(implicit configuration: GatlingConfiguration): BodyPart =
    BodyPart.stringBodyPart(Some(name), string, configuration.core.charset)

  def RawFileBodyPart(filePath: Expression[String])(implicit rawFileBodies: RawFileBodies): BodyPart =
    BodyPart.rawFileBodyPart(None, filePath, rawFileBodies)
  def RawFileBodyPart(name: Expression[String], filePath: Expression[String])(implicit rawFileBodies: RawFileBodies): BodyPart =
    BodyPart.rawFileBodyPart(Some(name), filePath, rawFileBodies)

  def PebbleStringBodyPart(string: String)(implicit configuration: GatlingConfiguration): BodyPart =
    BodyPart.pebbleStringBodyPart(None, string, configuration.core.charset)
  def PebbleStringBodyPart(name: Expression[String], string: String)(implicit configuration: GatlingConfiguration): BodyPart =
    BodyPart.pebbleStringBodyPart(Some(name), string, configuration.core.charset)

  def PebbleFileBodyPart(filePath: Expression[String])(implicit configuration: GatlingConfiguration, pebbleFileBodies: PebbleFileBodies): BodyPart =
    BodyPart.pebbleFileBodyPart(None, filePath, configuration.core.charset, pebbleFileBodies)
  def PebbleFileBodyPart(
      name: Expression[String],
      filePath: Expression[String]
  )(implicit configuration: GatlingConfiguration, pebbleFileBodies: PebbleFileBodies): BodyPart =
    BodyPart.pebbleFileBodyPart(Some(name), filePath, configuration.core.charset, pebbleFileBodies)

  def ByteArrayBodyPart(bytes: Expression[Array[Byte]]): BodyPart = BodyPart.byteArrayBodyPart(None, bytes)
  def ByteArrayBodyPart(name: Expression[String], bytes: Expression[Array[Byte]]): BodyPart = BodyPart.byteArrayBodyPart(Some(name), bytes)
}
