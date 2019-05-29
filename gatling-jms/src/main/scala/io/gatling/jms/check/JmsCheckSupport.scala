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

import javax.jms.Message

import scala.annotation.implicitNotFound

import io.gatling.core.check.{ CheckBuilder, CheckMaterializer, FindCheckBuilder, ValidatorCheckBuilder }
import io.gatling.core.check.extractor.xpath.{ Dom, XPathCheckType, XmlParsers }
import io.gatling.jms.JmsCheck

trait JmsCheckSupport {

  def simpleCheck: JmsSimpleCheck.type = JmsSimpleCheck

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for JMS.")
  implicit def checkBuilder2JmsCheck[A, P, X](checkBuilder: CheckBuilder[A, P, X])(implicit materializer: CheckMaterializer[A, JmsCheck, Message, P]): JmsCheck =
    checkBuilder.build(materializer)

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for JMS.")
  implicit def validatorCheckBuilder2JmsCheck[A, P, X](validatorCheckBuilder: ValidatorCheckBuilder[A, P, X])(implicit materializer: CheckMaterializer[A, JmsCheck, Message, P]): JmsCheck =
    validatorCheckBuilder.exists

  @implicitNotFound("Could not find a CheckMaterializer. This check might not be valid for JMS.")
  implicit def findCheckBuilder2JmsCheck[A, P, X](findCheckBuilder: FindCheckBuilder[A, P, X])(implicit materializer: CheckMaterializer[A, JmsCheck, Message, P]): JmsCheck =
    findCheckBuilder.find.exists

  implicit def jmsXPathmaterializer(implicit xmlParsers: XmlParsers): CheckMaterializer[XPathCheckType, JmsCheck, Message, Option[Dom]] = new JmsXPathCheckMaterializer(xmlParsers)
}
