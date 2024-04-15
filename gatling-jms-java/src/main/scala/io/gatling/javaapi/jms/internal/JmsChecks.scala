/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.jms.internal

import java.{ util => ju }

import scala.jdk.CollectionConverters._

import io.gatling.core.{ Predef => CorePredef }
import io.gatling.core.check.CheckBuilder
import io.gatling.core.check.bytes.BodyBytesCheckType
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.check.xpath.XPathCheckType
import io.gatling.javaapi.core.internal.CoreCheckType
import io.gatling.jms.{ Predef => JmsPredef }
import io.gatling.jms.JmsCheck

import com.fasterxml.jackson.databind.JsonNode
import net.sf.saxon.s9api.XdmNode

object JmsChecks {
  private def toScalaCheck(javaCheck: io.gatling.javaapi.core.CheckBuilder): JmsCheck = {
    val scalaCheck = javaCheck.asScala
    javaCheck.`type` match {
      case CoreCheckType.BodyBytes =>
        scalaCheck.asInstanceOf[CheckBuilder[BodyBytesCheckType, Array[Byte]]].build(JmsPredef.jmsBodyBytesCheckMaterializer(CorePredef.configuration))
      case CoreCheckType.BodyLength =>
        scalaCheck.asInstanceOf[CheckBuilder[BodyBytesCheckType, Int]].build(JmsPredef.jmsBodyLengthCheckMaterializer(CorePredef.configuration))
      case CoreCheckType.BodyString =>
        scalaCheck.asInstanceOf[CheckBuilder[BodyStringCheckType, String]].build(JmsPredef.jmsBodyStringCheckMaterializer(CorePredef.configuration))
      case CoreCheckType.Substring =>
        scalaCheck.asInstanceOf[CheckBuilder[SubstringCheckType, String]].build(JmsPredef.jmsSubstringCheckMaterializer(CorePredef.configuration))
      case CoreCheckType.XPath => scalaCheck.asInstanceOf[CheckBuilder[XPathCheckType, XdmNode]].build(JmsPredef.jmsXPathMaterializer)
      case CoreCheckType.JsonPath =>
        scalaCheck
          .asInstanceOf[CheckBuilder[JsonPathCheckType, JsonNode]]
          .build(JmsPredef.jmsJsonPathCheckMaterializer(CorePredef.defaultJsonParsers))
      case CoreCheckType.JmesPath =>
        scalaCheck
          .asInstanceOf[CheckBuilder[JmesPathCheckType, JsonNode]]
          .build(JmsPredef.jmsJmesPathCheckMaterializer(CorePredef.defaultJsonParsers))
      case JmsCheckType.Simple => scalaCheck.build(null).asInstanceOf[JmsCheck]

      case unknown => throw new IllegalArgumentException(s"JMS DSL doesn't support $unknown")
    }
  }

  def toScalaChecks(javaChecks: ju.List[io.gatling.javaapi.core.CheckBuilder]): Seq[JmsCheck] =
    javaChecks.asScala.map(toScalaCheck).toSeq
}
