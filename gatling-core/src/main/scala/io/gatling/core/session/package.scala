/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import scala.annotation.tailrec

import io.gatling.commons.util.Maps._
import io.gatling.commons.validation._
import io.gatling.core.session.el._

package object session {

  type Expression[T] = Session => Validation[T]

  val TrueExpressionSuccess = true.expressionSuccess

  case class StaticStringExpression(value: String) extends Expression[String] {
    val valueV = value.success
    def apply(session: Session) = valueV
  }

  implicit class ExpressionSuccessWrapper[T](val value: T) extends AnyVal {
    def expressionSuccess: Expression[T] = {
      val valueS = value.success
      _ => valueS
    }
  }

  implicit class ExpressionFailureWrapper(val message: String) extends AnyVal {
    def expressionFailure: Expression[Nothing] = {
      val valueS = message.failure
      _ => valueS
    }
  }

  implicit class RichExpression[T](val expression: Expression[T]) extends AnyVal {
    def map[U](f: T => U): Expression[U] = expression.andThen(_.map(f))
    def safe: Expression[T] = session => safely()(expression(session))
  }

  def resolveOptionalExpression[T](expression: Option[Expression[T]], session: Session): Validation[Option[T]] = expression match {
    case None      => NoneSuccess
    case Some(exp) => exp(session).map(Some.apply)
  }

  def resolveIterable[X](iterable: Iterable[(String, Expression[X])]): Expression[Seq[(String, X)]] = {

      @tailrec
      def resolveRec(session: Session, entries: Iterator[(String, Expression[X])], acc: List[(String, X)]): Validation[Seq[(String, X)]] = {
        if (entries.isEmpty)
          acc.reverse.success
        else {
          val (key, elValue) = entries.next()
          elValue(session) match {
            case Success(value)   => resolveRec(session, entries, (key -> value) :: acc)
            case failure: Failure => failure
          }
        }
      }

    (session: Session) => resolveRec(session, iterable.iterator, Nil)
  }

  def seq2SeqExpression(seq: Seq[(String, Any)]): Expression[Seq[(String, Any)]] = {
    val elValues: Seq[(String, Expression[Any])] = seq.map {
      case (key, value) =>
        val elValue = value match {
          case s: String => s.el[Any]
          case v         => v.expressionSuccess
        }
        key -> elValue
    }

    resolveIterable(elValues)
  }

  def map2SeqExpression(map: Map[String, Any]): Expression[Seq[(String, Any)]] = {
    val elValues: Map[String, Expression[Any]] = map.forceMapValues {
      case s: String => s.el[Any]
      case v         => v.expressionSuccess
    }

    resolveIterable(elValues)
  }
}
