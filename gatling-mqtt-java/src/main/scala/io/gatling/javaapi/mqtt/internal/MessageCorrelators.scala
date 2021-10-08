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

package io.gatling.javaapi.mqtt.internal

import io.gatling.core.{ Predef => CorePredef }
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.bytes.BodyBytesCheckType
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.javaapi.core.internal.CoreCheckType
import io.gatling.mqtt.{ Predef => MqttPredef }
import io.gatling.mqtt.check.MessageCorrelator

import com.fasterxml.jackson.databind.JsonNode

object MessageCorrelators {

  def toScalaCorrelator(javaCheck: io.gatling.javaapi.core.CheckBuilder): MessageCorrelator = {
    val scalaCheck = javaCheck.asScala
    javaCheck.`type` match {
      case CoreCheckType.BodyBytes =>
        val checkBuilder = scalaCheck.asInstanceOf[CheckBuilder[BodyBytesCheckType, Array[Byte]]]
        val textCheck = checkBuilder.build(MqttPredef.MqttTextBodyBytesCorrelatorMaterializer)
        val bufferCheck = checkBuilder.build(MqttPredef.MqttBufferBodyBytesCorrelatorMaterializer)
        MessageCorrelator(textCheck, bufferCheck)

      case CoreCheckType.BodyLength =>
        val checkBuilder = scalaCheck.asInstanceOf[CheckBuilder[BodyBytesCheckType, Int]]
        val textCheck = checkBuilder.build(MqttPredef.MqttTextBodyLengthCorrelatorMaterializer)
        val bufferCheck = checkBuilder.build(MqttPredef.MqttBufferBodyLengthCorrelatorMaterializer)
        MessageCorrelator(textCheck, bufferCheck)

      case CoreCheckType.BodyString =>
        val checkBuilder = scalaCheck.asInstanceOf[CheckBuilder[BodyStringCheckType, String]]
        val textCheck = checkBuilder.build(MqttPredef.MqttTextBodyStringCorrelatorMaterializer)
        val bufferCheck = checkBuilder.build(MqttPredef.MqttBufferBodyStringCorrelatorMaterializer)
        MessageCorrelator(textCheck, bufferCheck)

      case CoreCheckType.Substring =>
        val checkBuilder = scalaCheck.asInstanceOf[CheckBuilder[SubstringCheckType, String]]
        val textCheck = checkBuilder.build(MqttPredef.MqttTextSubstringCorrelatorMaterializer)
        val bufferCheck = checkBuilder.build(MqttPredef.MqttBufferSubstringCorrelatorMaterializer)
        MessageCorrelator(textCheck, bufferCheck)

      case CoreCheckType.Regex =>
        val checkBuilder = scalaCheck.asInstanceOf[CheckBuilder[RegexCheckType, String]]
        val textCheck = checkBuilder.build(MqttPredef.MqttTextRegexCorrelatorMaterializer)
        val bufferCheck = checkBuilder.build(MqttPredef.MqttBufferRegexCorrelatorMaterializer)
        MessageCorrelator(textCheck, bufferCheck)

      case CoreCheckType.JsonPath =>
        val checkBuilder = scalaCheck.asInstanceOf[CheckBuilder[JsonPathCheckType, JsonNode]]
        val textCheck = checkBuilder.build(MqttPredef.mqttTextJsonPathMaterializer(CorePredef.defaultJsonParsers))
        val bufferCheck = checkBuilder.build(MqttPredef.mqttBufferJsonPathMaterializer(CorePredef.defaultJsonParsers))
        MessageCorrelator(textCheck, bufferCheck)

      case CoreCheckType.JmesPath =>
        val checkBuilder = scalaCheck.asInstanceOf[CheckBuilder[JmesPathCheckType, JsonNode]]
        val textCheck = checkBuilder.build(MqttPredef.mqttTextJmesPathMaterializer(CorePredef.defaultJsonParsers))
        val bufferCheck = checkBuilder.build(MqttPredef.mqttBufferJmesPathMaterializer(CorePredef.defaultJsonParsers))
        MessageCorrelator(textCheck, bufferCheck)

      case unknown => throw new IllegalArgumentException(s"MQTT DSL doesn't support $unknown")
    }
  }
}
