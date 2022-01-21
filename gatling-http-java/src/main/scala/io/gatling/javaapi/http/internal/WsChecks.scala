/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.http.internal

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
import io.gatling.http.check.ws.{ WsCheck, WsCheckMaterializer }
import io.gatling.javaapi.core.internal.CoreCheckType

import com.fasterxml.jackson.databind.JsonNode

object WsChecks {

  private def toScalaTextCheck(javaCheck: io.gatling.javaapi.core.CheckBuilder): WsCheck.Text = {
    val scalaCheck = javaCheck.asScala
    javaCheck.`type` match {
      case CoreCheckType.BodyString => scalaCheck.asInstanceOf[CheckBuilder[BodyStringCheckType, String]].build(WsCheckMaterializer.Text.BodyString)
      case CoreCheckType.Regex      => scalaCheck.asInstanceOf[CheckBuilder[RegexCheckType, String]].build(WsCheckMaterializer.Text.Regex)
      case CoreCheckType.Substring  => scalaCheck.asInstanceOf[CheckBuilder[SubstringCheckType, String]].build(WsCheckMaterializer.Text.Substring)
      case CoreCheckType.JsonPath =>
        scalaCheck.asInstanceOf[CheckBuilder[JsonPathCheckType, JsonNode]].build(WsCheckMaterializer.Text.jsonPath(CorePredef.defaultJsonParsers))
      case CoreCheckType.JmesPath =>
        scalaCheck.asInstanceOf[CheckBuilder[JmesPathCheckType, JsonNode]].build(WsCheckMaterializer.Text.jmesPath(CorePredef.defaultJsonParsers))
      case unknown => throw new IllegalArgumentException(s"WebSocket DSL doesn't support text check $unknown")
    }
  }

  def toScalaTextChecks(javaChecks: ju.List[io.gatling.javaapi.core.CheckBuilder]): Seq[WsCheck.Text] =
    javaChecks.asScala.map(toScalaTextCheck).toSeq

  private def toScalaBinaryCheck(javaCheck: io.gatling.javaapi.core.CheckBuilder): WsCheck.Binary = {
    val scalaCheck = javaCheck.asScala
    javaCheck.`type` match {
      case CoreCheckType.BodyBytes  => scalaCheck.asInstanceOf[CheckBuilder[BodyBytesCheckType, Array[Byte]]].build(WsCheckMaterializer.Binary.BodyBytes)
      case CoreCheckType.BodyLength => scalaCheck.asInstanceOf[CheckBuilder[BodyBytesCheckType, Int]].build(WsCheckMaterializer.Binary.BodyLength)
      case unknown                  => throw new IllegalArgumentException(s"WebSocket DSL doesn't support binary check $unknown")
    }
  }

  def toScalaBinaryChecks(javaChecks: ju.List[io.gatling.javaapi.core.CheckBuilder]): Seq[WsCheck.Binary] =
    javaChecks.asScala.map(toScalaBinaryCheck).toSeq
}
