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

package io.gatling.mqtt.javaapit.internal

import java.{ util => ju }

import scala.jdk.CollectionConverters._

import io.gatling.core.{ Predef => CorePredef }
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.bytes.BodyBytesCheckType
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.javaapi.internal.CoreCheckType
import io.gatling.mqtt.{ Predef => MqttPredef }
import io.gatling.mqtt.check.MqttCheck

import com.fasterxml.jackson.databind.JsonNode

object MqttChecks {

  private def toScalaCheck(javaCheck: io.gatling.core.javaapi.CheckBuilder): MqttCheck = {
    val scalaCheck = javaCheck.asScala
    javaCheck.`type` match {
      case CoreCheckType.BodyBytes =>
        scalaCheck.asInstanceOf[CheckBuilder[BodyBytesCheckType, Array[Byte]]].build(MqttPredef.MqttBufferBodyBytesCorrelatorMaterializer)

      case CoreCheckType.BodyLength =>
        scalaCheck.asInstanceOf[CheckBuilder[BodyBytesCheckType, Int]].build(MqttPredef.MqttBufferBodyLengthCorrelatorMaterializer)

      case CoreCheckType.BodyString =>
        scalaCheck.asInstanceOf[CheckBuilder[BodyStringCheckType, String]].build(MqttPredef.MqttBufferBodyStringCorrelatorMaterializer)

      case CoreCheckType.Substring =>
        scalaCheck.asInstanceOf[CheckBuilder[SubstringCheckType, String]].build(MqttPredef.MqttBufferSubstringCorrelatorMaterializer)

      case CoreCheckType.Regex =>
        scalaCheck.asInstanceOf[CheckBuilder[RegexCheckType, String]].build(MqttPredef.MqttBufferRegexCorrelatorMaterializer)

      case CoreCheckType.JsonPath =>
        scalaCheck.asInstanceOf[CheckBuilder[JsonPathCheckType, JsonNode]].build(MqttPredef.mqttBufferJsonPathMaterializer(CorePredef.defaultJsonParsers))

      case CoreCheckType.JmesPath =>
        scalaCheck.asInstanceOf[CheckBuilder[JmesPathCheckType, JsonNode]].build(MqttPredef.mqttBufferJmesPathMaterializer(CorePredef.defaultJsonParsers))

      case unknown => throw new IllegalArgumentException(s"MQTT DSL doesn't support $unknown")
    }
  }

  def toScalaChecks(javaChecks: ju.List[io.gatling.core.javaapi.CheckBuilder]): Seq[MqttCheck] =
    javaChecks.asScala.map(toScalaCheck).toSeq
}
