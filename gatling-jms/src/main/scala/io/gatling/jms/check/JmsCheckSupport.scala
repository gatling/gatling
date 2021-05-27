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

package io.gatling.jms.check

import javax.jms.Message

import scala.annotation.implicitNotFound

import io.gatling.commons.validation.{ FailureWrapper, Validation }
import io.gatling.core.check._
import io.gatling.core.check.Check.PreparedCache
import io.gatling.core.check.bytes.BodyBytesCheckType
import io.gatling.core.check.jmespath.JmesPathCheckType
import io.gatling.core.check.jsonpath.JsonPathCheckType
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.check.xpath._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers
import io.gatling.core.session.{ Expression, Session }
import io.gatling.jms.JmsCheck

import com.fasterxml.jackson.databind.JsonNode
import net.sf.saxon.s9api.XdmNode

trait JmsCheckSupport {
  def simpleCheck(f: Message => Boolean): JmsCheck =
    JmsCheck(
      (response: Message, _: Session, _: PreparedCache) =>
        if (f(response)) {
          CheckResult.NoopCheckResultSuccess
        } else {
          "JMS check failed".failure
        },
      None
    )

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for JMS.")
  implicit def checkBuilder2JmsCheck[T, P, X](
      checkBuilder: CheckBuilder[T, P, X]
  )(implicit materializer: CheckMaterializer[T, JmsCheck, Message, P]): JmsCheck =
    checkBuilder.build(materializer)

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for JMS.")
  implicit def validatorCheckBuilder2JmsCheck[T, P, X](
      validatorCheckBuilder: ValidatorCheckBuilder[T, P, X]
  )(implicit materializer: CheckMaterializer[T, JmsCheck, Message, P]): JmsCheck =
    validatorCheckBuilder.exists

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for JMS.")
  implicit def findCheckBuilder2JmsCheck[T, P, X](
      findCheckBuilder: FindCheckBuilder[T, P, X]
  )(implicit materializer: CheckMaterializer[T, JmsCheck, Message, P]): JmsCheck =
    findCheckBuilder.find.exists

  implicit def jmsBodyBytesCheckMaterializer(implicit
      configuration: GatlingConfiguration
  ): CheckMaterializer[BodyBytesCheckType, JmsCheck, Message, Array[Byte]] =
    JmsCheckMaterializer.bodyBytes(configuration.core.charset)

  implicit def jmsBodyLengthCheckMaterializer(implicit
      configuration: GatlingConfiguration
  ): CheckMaterializer[BodyBytesCheckType, JmsCheck, Message, Int] =
    JmsCheckMaterializer.bodyLength(configuration.core.charset)

  implicit def jmsBodyStringCheckMaterializer(implicit
      configuration: GatlingConfiguration
  ): CheckMaterializer[BodyStringCheckType, JmsCheck, Message, String] =
    JmsCheckMaterializer.bodyString(configuration.core.charset)

  implicit def jmsBodySubstringCheckMaterializer(implicit
      configuration: GatlingConfiguration
  ): CheckMaterializer[SubstringCheckType, JmsCheck, Message, String] =
    JmsCheckMaterializer.substring(configuration.core.charset)

  implicit val jmsXPathmaterializer: CheckMaterializer[XPathCheckType, JmsCheck, Message, Option[XdmNode]] =
    JmsCheckMaterializer.Xpath

  implicit def jmsJsonPathCheckMaterializer(implicit
      jsonParsers: JsonParsers,
      configuration: GatlingConfiguration
  ): CheckMaterializer[JsonPathCheckType, JmsCheck, Message, JsonNode] =
    JmsCheckMaterializer.jsonPath(jsonParsers, configuration.core.charset)

  implicit def jmsJmesPathCheckMaterializer(implicit
      jsonParsers: JsonParsers,
      configuration: GatlingConfiguration
  ): CheckMaterializer[JmesPathCheckType, JmsCheck, Message, JsonNode] =
    JmsCheckMaterializer.jmesPath(jsonParsers, configuration.core.charset)

  implicit val jmsTypedConditionalCheckWrapper: TypedConditionalCheckWrapper[Message, JmsCheck] =
    (condition: (Message, Session) => Validation[Boolean], thenCheck: JmsCheck) => thenCheck.copy(condition = Some(condition))

  implicit val jmsUntypedConditionalCheckWrapper: UntypedConditionalCheckWrapper[JmsCheck] =
    (condition: Expression[Boolean], thenCheck: JmsCheck) => thenCheck.withUntypedCondition(condition)
}
