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

package io.gatling.mqtt.check

import scala.annotation.implicitNotFound

import io.gatling.core.check._
import io.gatling.core.check.bytes.BodyBytesCheckType
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.json.JsonParsers

import com.fasterxml.jackson.databind.JsonNode
import io.netty.buffer.ByteBuf

trait MqttCheckSupport {

  // materializers
  implicit def mqttTextJmesPathMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JmesPathCheckType, Check[String], String, JsonNode] = ???

  implicit def mqttBufferJmesPathMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JmesPathCheckType, Check[ByteBuf], ByteBuf, JsonNode] = ???

  implicit def mqttTextJsonPathMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, Check[String], String, JsonNode] = ???

  implicit def mqttBufferJsonPathMaterializer(implicit jsonParsers: JsonParsers): CheckMaterializer[JsonPathCheckType, Check[ByteBuf], ByteBuf, JsonNode] = ???

  implicit val MqttTextRegexCorrelatorMaterializer: CheckMaterializer[RegexCheckType, Check[String], String, String] = ???

  implicit val MqttBufferRegexCorrelatorMaterializer: CheckMaterializer[RegexCheckType, Check[ByteBuf], ByteBuf, String] = ???

  implicit val MqttTextBodyStringCorrelatorMaterializer: CheckMaterializer[BodyStringCheckType, Check[String], String, String] = ???

  implicit val MqttBufferBodyStringCorrelatorMaterializer: CheckMaterializer[SubstringCheckType, Check[ByteBuf], ByteBuf, String] = ???

  implicit val MqttTextBodySubstringCorrelatorMaterializer: CheckMaterializer[SubstringCheckType, Check[String], String, String] = ???

  implicit val MqttBufferBodySubstringCorrelatorMaterializer: CheckMaterializer[BodyStringCheckType, Check[ByteBuf], ByteBuf, String] = ???

  implicit val MqttTextBodyBytesCorrelatorMaterializer: CheckMaterializer[BodyBytesCheckType, Check[String], String, Array[Byte]] = ???

  implicit val MqttBufferBodyBytesCorrelatorMaterializer: CheckMaterializer[BodyBytesCheckType, Check[ByteBuf], ByteBuf, Array[Byte]] = ???

  // checks
  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for MQTT.")
  implicit def checkBuilder2MqttCheck[A, P, X](checkBuilder: CheckBuilder[A, P, X]) //
  (implicit materializer: CheckMaterializer[A, MqttCheck, ByteBuf, P]): MqttCheck =
    checkBuilder.build(materializer)

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for MQTT.")
  implicit def validatorCheckBuilder2MqttCheck[A, P, X](validatorCheckBuilder: ValidatorCheckBuilder[A, P, X]) //
  (implicit materializer: CheckMaterializer[A, MqttCheck, ByteBuf, P]): MqttCheck =
    validatorCheckBuilder.exists

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for MQTT.")
  implicit def findCheckBuilder2MqttCheck[A, P, X](findCheckBuilder: FindCheckBuilder[A, P, X]) //
  (implicit materializer: CheckMaterializer[A, MqttCheck, ByteBuf, P]): MqttCheck =
    findCheckBuilder.find.exists

  // correlators
  implicit def findCheckBuilder2MessageCorrelator[A, P](findCheckBuilder: FindCheckBuilder[A, P, String]) //
  (implicit
      textMaterializer: CheckMaterializer[A, Check[String], String, P],
      bufferMaterializer: CheckMaterializer[A, Check[ByteBuf], ByteBuf, P]
  ): MessageCorrelator =
    validatorCheckBuilder2MessageCorrelator(findCheckBuilder.find)

  implicit def validatorCheckBuilder2MessageCorrelator[A, P](validatorCheckBuilder: ValidatorCheckBuilder[A, P, String]) //
  (implicit
      textMaterializer: CheckMaterializer[A, Check[String], String, P],
      bufferMaterializer: CheckMaterializer[A, Check[ByteBuf], ByteBuf, P]
  ): MessageCorrelator = ???
}
