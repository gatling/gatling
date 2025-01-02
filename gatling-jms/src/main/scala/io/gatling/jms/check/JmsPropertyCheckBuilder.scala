/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.validation._
import io.gatling.commons.validation.{ Success, Validation }
import io.gatling.core.check.{ CheckBuilder, FindCriterionExtractor }
import io.gatling.core.session._

sealed trait LowPriorityJmsPropertyFilterImplicits {
  private def newJmsPropertyFilter[T](f: (Message, String) => T): JmsPropertyFilter[T] =
    new JmsPropertyFilter[T] {
      override val filter: (Message, String) => Validation[Option[T]] =
        (message, propertyName) =>
          safely() {
            if (message.propertyExists(propertyName)) {
              Success(Some(f(message, propertyName)))
            } else {
              Validation.NoneSuccess
            }
          }
    }

  implicit val stringJmsPropertyFilter: JmsPropertyFilter[String] =
    newJmsPropertyFilter((message, propertyName) => message.getStringProperty(propertyName))

  implicit val jBooleanJmsPropertyFilter: JmsPropertyFilter[Boolean] =
    newJmsPropertyFilter((message, propertyName) => message.getBooleanProperty(propertyName))

  implicit val jIntegerJmsPropertyFilter: JmsPropertyFilter[Int] =
    newJmsPropertyFilter((message, propertyName) => message.getIntProperty(propertyName))

  implicit val jLongJmsPropertyFilter: JmsPropertyFilter[Long] =
    newJmsPropertyFilter((message, propertyName) => message.getLongProperty(propertyName))

  implicit val jDoubleJmsPropertyFilter: JmsPropertyFilter[Double] =
    newJmsPropertyFilter((message, propertyName) => message.getDoubleProperty(propertyName))

  implicit val jFloatJmsPropertyFilter: JmsPropertyFilter[Float] =
    newJmsPropertyFilter((message, propertyName) => message.getFloatProperty(propertyName))

  implicit val jByteJmsPropertyFilter: JmsPropertyFilter[Byte] =
    newJmsPropertyFilter((message, propertyName) => message.getByteProperty(propertyName))

  implicit val jShortJmsPropertyFilter: JmsPropertyFilter[Short] =
    newJmsPropertyFilter((message, propertyName) => message.getShortProperty(propertyName))

  implicit val jAnyJmsPropertyFilter: JmsPropertyFilter[Any] =
    newJmsPropertyFilter((message, propertyName) => message.getObjectProperty(propertyName))
}

object JmsPropertyFilter extends LowPriorityJmsPropertyFilterImplicits {
  def apply[X: JmsPropertyFilter]: JmsPropertyFilter[X] = implicitly[JmsPropertyFilter[X]]
}

@implicitNotFound("No member of type class JmsPropertyFilter found for type ${X}")
trait JmsPropertyFilter[X] {
  def filter: (Message, String) => Validation[Option[X]]
}

final class JmsPropertyExtractor[X: JmsPropertyFilter](name: String, propertyName: String)
    extends FindCriterionExtractor[Message, String, X](name, propertyName, 0, JmsPropertyFilter[X].filter(_, propertyName))

sealed trait JmsPropertyCheckType

sealed trait JmsPropertyOfType {
  self: JmsPropertyCheckBuilder[String] =>

  def ofType[X: JmsPropertyFilter]: CheckBuilder.Find[JmsPropertyCheckType, Message, X] = new JmsPropertyCheckBuilder(propertyName)
}

object JmsPropertyCheckBuilder {
  def jmsProperty(name: String): JmsPropertyCheckBuilder[String] with JmsPropertyOfType =
    new JmsPropertyCheckBuilder[String](name) with JmsPropertyOfType
}

class JmsPropertyCheckBuilder[X: JmsPropertyFilter] private[check] (
    private[check] val propertyName: String
) extends CheckBuilder.Find.Default[JmsPropertyCheckType, Message, X](
      new JmsPropertyExtractor("jmsProperty", propertyName).expressionSuccess,
      displayActualValue = true
    )
