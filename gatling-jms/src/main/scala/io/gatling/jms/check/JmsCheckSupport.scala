/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import java.util.{ Map => JMap }

import io.gatling.commons.validation.Validation
import io.gatling.core.check.string.BodyStringCheckType
import io.gatling.core.check.substring.SubstringCheckType
import io.gatling.core.check.xpath.{ Dom, XPathCheckType, XmlParsers }
import io.gatling.core.check.{
  Check,
  CheckBuilder,
  CheckMaterializer,
  CheckResult,
  ConditionalCheck,
  FindCheckBuilder,
  TypedConditionalCheckWrapper,
  UntypedConditionalCheckWrapper,
  ValidatorCheckBuilder
}
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.json.JsonParsers
import io.gatling.core.session.{ Expression, Session }
import io.gatling.jms.JmsCheck
import javax.jms.Message

import scala.annotation.implicitNotFound

trait JmsCheckSupport {
  def simpleCheck: JmsSimpleCheck.type = JmsSimpleCheck

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for JMS.")
  implicit def checkBuilder2JmsCheck[A, P, X](
      checkBuilder: CheckBuilder[A, P, X]
  )(implicit materializer: CheckMaterializer[A, JmsCheck, Message, P]): JmsCheck =
    checkBuilder.build(materializer)

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for JMS.")
  implicit def validatorCheckBuilder2JmsCheck[A, P, X](
      validatorCheckBuilder: ValidatorCheckBuilder[A, P, X]
  )(implicit materializer: CheckMaterializer[A, JmsCheck, Message, P]): JmsCheck =
    validatorCheckBuilder.exists

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for JMS.")
  implicit def findCheckBuilder2JmsCheck[A, P, X](
      findCheckBuilder: FindCheckBuilder[A, P, X]
  )(implicit materializer: CheckMaterializer[A, JmsCheck, Message, P]): JmsCheck =
    findCheckBuilder.find.exists

  implicit def jmsBodyStringCheckMaterializer(
      implicit gatlingConfiguration: GatlingConfiguration
  ): CheckMaterializer[BodyStringCheckType, JmsCheck, Message, String] =
    JmsBodyStringCheckMaterializer(gatlingConfiguration)

  implicit def jmsBodySubstringCheckMaterializer(
      implicit gatlingConfiguration: GatlingConfiguration
  ): CheckMaterializer[SubstringCheckType, JmsCheck, Message, String] =
    JmsBodySubstringCheckMaterializer(gatlingConfiguration)

  implicit def jmsXPathmaterializer(implicit xmlParsers: XmlParsers): CheckMaterializer[XPathCheckType, JmsCheck, Message, Option[Dom]] =
    new JmsXPathCheckMaterializer(xmlParsers)

  implicit def jmsJsonPathCheckMaterializer(implicit jsonParsers: JsonParsers, gatlingConfiguration: GatlingConfiguration): JmsJsonPathCheckMaterializer =
    new JmsJsonPathCheckMaterializer(jsonParsers, gatlingConfiguration)

  implicit val jmsUntypedConditionalCheckWrapper: UntypedConditionalCheckWrapper[JmsCheck] =
    (condition: Expression[Boolean], thenCheck: JmsCheck) =>
      new Check[Message] {
        private val typedCondition = (_: Message, ses: Session) => condition(ses)

        override def check(response: Message, session: Session, preparedCache: JMap[Any, Any]): Validation[CheckResult] =
          ConditionalCheck(typedCondition, thenCheck).check(response, session, preparedCache)
      }

  implicit val jmsTypedConditionalCheckWrapper: TypedConditionalCheckWrapper[Message, JmsCheck] =
    (condition: (Message, Session) => Validation[Boolean], thenCheck: JmsCheck) =>
      (response: Message, session: Session, preparedCache: JMap[Any, Any]) => ConditionalCheck(condition, thenCheck).check(response, session, preparedCache)

}
