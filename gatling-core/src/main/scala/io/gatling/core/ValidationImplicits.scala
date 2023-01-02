/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.core

import scala.language.reflectiveCalls
import scala.reflect.ClassTag

import io.gatling.commons.Exclude
import io.gatling.commons.util.TypeCaster
import io.gatling.commons.validation._
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session._
import io.gatling.core.session.el._
import io.gatling.core.structure.{ ChainBuilder, ScenarioBuilder }

sealed trait NonValidable

object NonValidable {
  implicit val sessionAttributeIsNonValidable1, sessionAttributeIsNonValidable2: Exclude[NonValidable, SessionAttribute] =
    Exclude[NonValidable, SessionAttribute]
  implicit val chainBuilderIsNonValidable1, chainBuilderIsNonValidable2: Exclude[NonValidable, ChainBuilder] = Exclude[NonValidable, ChainBuilder]
  implicit val scenarioBuilderIsNonValidable1, scenarioBuilderIsNonValidable12: Exclude[NonValidable, ScenarioBuilder] = Exclude[NonValidable, ScenarioBuilder]
  implicit val actionBuilderIsNonValidable1, actionBuilderIsNonValidable2: Exclude[NonValidable, ActionBuilder] = Exclude[NonValidable, ActionBuilder]

  // Partially apply the type to be compatible with context bounds
  type DoesNotContain[X] = Exclude[NonValidable, X]
}

sealed trait NeitherValidableNorString

object NeitherValidableNorString {
  implicit val sessionAttributeIsNeitherValidableNorString1, sessionAttributeIsNonValidable_2: Exclude[NeitherValidableNorString, SessionAttribute] =
    Exclude[NeitherValidableNorString, SessionAttribute]
  implicit val chainBuilderIsNeitherValidableNorString1, chainBuilderIsNeitherValidableNorString2: Exclude[NeitherValidableNorString, ChainBuilder] =
    Exclude[NeitherValidableNorString, ChainBuilder]
  implicit val scenarioBuilderIsNeitherValidableNorString1, scenarioBuilderIsNeitherValidableNorString2: Exclude[NeitherValidableNorString, ScenarioBuilder] =
    Exclude[NeitherValidableNorString, ScenarioBuilder]
  implicit val actionBuilderIsNeitherValidableNorString1, actionBuilderIsNeitherValidableNorString2: Exclude[NeitherValidableNorString, ActionBuilder] =
    Exclude[NeitherValidableNorString, ActionBuilder]
  implicit val stringIsNeitherValidableNorString1, stringIsNeitherValidableNorString2: Exclude[NeitherValidableNorString, String] =
    Exclude[NeitherValidableNorString, String]

  // Partially apply the type to be compatible with context bounds
  type DoesNotContain[X] = Exclude[NeitherValidableNorString, X]
}

class NoUnexpectedValidationLifting[T](value: T) {
  def map[A](f: T => A): Validation[A] = throw new UnsupportedOperationException("Not supposed to be ever called")
  def flatMap[A](f: T => Validation[A]): Validation[A] = throw new UnsupportedOperationException("Not supposed to be ever called")
  def mapError(f: String => String): Validation[T] = throw new UnsupportedOperationException("Not supposed to be ever called")
  def foreach(f: T => Any): Unit = throw new UnsupportedOperationException("Not supposed to be ever called")
  def withFilter(p: T => Boolean): Validation[T] = throw new UnsupportedOperationException("Not supposed to be ever called")
  def filter(p: T => Boolean): Validation[T] = throw new UnsupportedOperationException("Not supposed to be ever called")
  def onSuccess(f: T => Any): Unit = throw new UnsupportedOperationException("Not supposed to be ever called")
  def onFailure(f: String => Any): Unit = throw new UnsupportedOperationException("Not supposed to be ever called")
  def recover[A >: T](v: => A): Validation[A] = throw new UnsupportedOperationException("Not supposed to be ever called")
  def toOption: Option[T] = throw new UnsupportedOperationException("Not supposed to be ever called")
}

trait ValidationImplicits {
  implicit def stringToExpression[T: TypeCaster: ClassTag](string: String): Expression[T] = string.el
  implicit def value2Success[T: NonValidable.DoesNotContain](value: T): Validation[T] = value.success
  implicit def value2Expression[T: NeitherValidableNorString.DoesNotContain](value: T): Expression[T] = value.expressionSuccess
  implicit def function2Expression[T](f: Session => T): Expression[T] = session => safely()(f(session).success)
  implicit def value2NoUnexpectedValidationLifting[T: NonValidable.DoesNotContain](value: T): NoUnexpectedValidationLifting[T] =
    new NoUnexpectedValidationLifting(value)
}
