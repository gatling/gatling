/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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
import java.io.InputStream

import scala.jdk.CollectionConverters._

import io.gatling.core.{ Predef => CorePredef }
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.bytes.BodyBytesCheckType
import io.gatling.core.check.checksum.{ Md5CheckType, Sha1CheckType }
import io.gatling.core.check.css.CssCheckType
import io.gatling.core.check.jmespath.{ JmesPathCheckType, JsonpJmesPathCheckType }
import io.gatling.core.check.jsonpath.{ JsonPathCheckType, JsonpJsonPathCheckType }
import io.gatling.core.check.regex.RegexCheckType
import io.gatling.core.check.stream.BodyStreamCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.check.time.ResponseTimeCheckType
import io.gatling.core.check.xpath.XPathCheckType
import io.gatling.core.stats.message.ResponseTimings
import io.gatling.http.{ Predef => HttpPredef }
import io.gatling.http.check.HttpCheck
import io.gatling.http.check.header.{ HttpHeaderCheckType, HttpHeaderRegexCheckType }
import io.gatling.http.check.status.HttpStatusCheckType
import io.gatling.http.check.url.{ CurrentLocationCheckType, CurrentLocationRegexCheckType }
import io.gatling.http.response.Response
import io.gatling.javaapi.core.internal.CoreCheckType

import com.fasterxml.jackson.databind.JsonNode
import jodd.lagarto.dom.NodeSelector
import net.sf.saxon.s9api.XdmNode

object HttpChecks {
  private def toScalaCheck(javaCheck: io.gatling.javaapi.core.CheckBuilder): HttpCheck = {
    val scalaCheck = javaCheck.asScala
    javaCheck.`type` match {
      case CoreCheckType.BodyBytes  => scalaCheck.asInstanceOf[CheckBuilder[BodyBytesCheckType, Array[Byte]]].build(HttpPredef.httpBodyBytesCheckMaterializer)
      case CoreCheckType.BodyLength => scalaCheck.asInstanceOf[CheckBuilder[BodyBytesCheckType, Int]].build(HttpPredef.httpBodyLengthCheckMaterializer)
      case CoreCheckType.BodyString => scalaCheck.asInstanceOf[CheckBuilder[BodyStringCheckType, String]].build(HttpPredef.httpBodyStringCheckMaterializer)
      case CoreCheckType.BodyStream =>
        scalaCheck.asInstanceOf[CheckBuilder[BodyStreamCheckType, () => InputStream]].build(HttpPredef.httpBodyStreamCheckMaterializer)
      case CoreCheckType.Regex     => scalaCheck.asInstanceOf[CheckBuilder[RegexCheckType, String]].build(HttpPredef.httpBodyRegexCheckMaterializer)
      case CoreCheckType.Substring => scalaCheck.asInstanceOf[CheckBuilder[SubstringCheckType, String]].build(HttpPredef.httpBodySubstringCheckMaterializer)
      case CoreCheckType.XPath     => scalaCheck.asInstanceOf[CheckBuilder[XPathCheckType, XdmNode]].build(HttpPredef.httpBodyXPathCheckMaterializer)
      case CoreCheckType.Css =>
        scalaCheck.asInstanceOf[CheckBuilder[CssCheckType, NodeSelector]].build(HttpPredef.httpBodyCssCheckMaterializer(CorePredef.defaultCssSelectors))
      case CoreCheckType.JsonPath =>
        scalaCheck.asInstanceOf[CheckBuilder[JsonPathCheckType, JsonNode]].build(HttpPredef.httpBodyJsonPathCheckMaterializer(CorePredef.defaultJsonParsers))
      case CoreCheckType.JsonpJsonPath =>
        scalaCheck
          .asInstanceOf[CheckBuilder[JsonpJsonPathCheckType, JsonNode]]
          .build(HttpPredef.httpBodyJsonpJsonPathCheckMaterializer(CorePredef.defaultJsonParsers))
      case CoreCheckType.JmesPath =>
        scalaCheck.asInstanceOf[CheckBuilder[JmesPathCheckType, JsonNode]].build(HttpPredef.httpBodyJmesPathCheckMaterializer(CorePredef.defaultJsonParsers))
      case CoreCheckType.JsonpJmesPath =>
        scalaCheck
          .asInstanceOf[CheckBuilder[JsonpJmesPathCheckType, JsonNode]]
          .build(HttpPredef.httpBodyJsonpJmesPathCheckMaterializer(CorePredef.defaultJsonParsers))
      case CoreCheckType.Md5  => scalaCheck.asInstanceOf[CheckBuilder[Md5CheckType, String]].build(HttpPredef.httpMd5CheckMaterializer)
      case CoreCheckType.Sha1 => scalaCheck.asInstanceOf[CheckBuilder[Sha1CheckType, String]].build(HttpPredef.httpSha1CheckMaterializer)
      case CoreCheckType.ResponseTime =>
        scalaCheck.asInstanceOf[CheckBuilder[ResponseTimeCheckType, ResponseTimings]].build(HttpPredef.httpResponseTimeCheckMaterializer)
      case HttpCheckType.CurrentLocation =>
        scalaCheck.asInstanceOf[CheckBuilder[CurrentLocationCheckType, String]].build(HttpPredef.currentLocationCheckMaterializer)
      case HttpCheckType.CurrentLocationRegex =>
        scalaCheck.asInstanceOf[CheckBuilder[CurrentLocationRegexCheckType, String]].build(HttpPredef.currentLocationRegexCheckMaterializer)
      case HttpCheckType.Status => scalaCheck.asInstanceOf[CheckBuilder[HttpStatusCheckType, Response]].build(HttpPredef.httpStatusCheckMaterializer)
      case HttpCheckType.Header => scalaCheck.asInstanceOf[CheckBuilder[HttpHeaderCheckType, Response]].build(HttpPredef.httpHeaderCheckMaterializer)
      case HttpCheckType.HeaderRegex =>
        scalaCheck.asInstanceOf[CheckBuilder[HttpHeaderRegexCheckType, Response]].build(HttpPredef.httpHeaderRegexCheckMaterializer)
      case unknown => throw new IllegalArgumentException(s"HTTP DSL doesn't support $unknown")
    }
  }

  def toScalaChecks(javaChecks: ju.List[io.gatling.javaapi.core.CheckBuilder]): Seq[HttpCheck] =
    javaChecks.asScala.map(toScalaCheck).toSeq
}
