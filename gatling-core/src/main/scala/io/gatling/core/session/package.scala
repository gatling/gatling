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

package io.gatling.core

import scala.annotation.tailrec

import io.gatling.commons.validation._
import io.gatling.core.session.el._

package object session {

  type Expression[T] = Session => Validation[T]

  val TrueExpressionSuccess: Expression[Boolean] = true.expressionSuccess
  val EmptyStringExpressionSuccess: Expression[String] = "".expressionSuccess

  final case class StaticValueExpression[T](value: T) extends Expression[T] {
    private val valueV = value.success

    override def apply(session: Session): Validation[T] = valueV
  }

  implicit class ExpressionSuccessWrapper[T](val value: T) extends AnyVal {
    def expressionSuccess: Expression[T] = StaticValueExpression(value)
  }

  implicit class ExpressionFailureWrapper(val message: String) extends AnyVal {
    def expressionFailure: Expression[Nothing] = {
      val valueS = message.failure
      _ => valueS
    }
  }

  implicit class RichExpression[T](val expression: Expression[T]) extends AnyVal {
    def map[U](f: T => U): Expression[U] = expression(_).map(f)
    def safe: Expression[T] = session => safely()(expression(session))
  }

  def resolveOptionalExpression[T](expression: Option[Expression[T]], session: Session): Validation[Option[T]] = expression match {
    case Some(exp) => exp(session).map(Some.apply)
    case _         => Validation.NoneSuccess
  }

  def expressionSeq2SeqExpression[X](iterable: Iterable[(Expression[X], Expression[X])]): Expression[Seq[(String, X)]] = {

    @tailrec
    def resolveRec(session: Session, entries: Iterator[(Expression[X], Expression[X])], acc: List[(String, X)]): Validation[Seq[(String, X)]] =
      if (entries.isEmpty) {
        acc.reverse.success
      } else {
        val (elKey, elValue) = entries.next()

        elKey(session) match {
          case Success(key) =>
            elValue(session) match {
              case Success(value)   => resolveRec(session, entries, key.toString -> value :: acc)
              case failure: Failure => failure
            }
          case failure: Failure => failure
        }
      }

    resolveRec(_, iterable.iterator, Nil)
  }

  def tupleSeq2SeqExpression(seq: Seq[(String, Any)]): Expression[Seq[(String, Any)]] = {
    val expressionSequence: Seq[(Expression[String], Expression[Any])] = seq.map { case (key, value) =>
      val keyExpression = key.el[String]
      val valueExpression = value match {
        case s: String => s.el[Any]
        case v         => v.expressionSuccess
      }

      keyExpression -> valueExpression
    }

    expressionSeq2SeqExpression(expressionSequence)
  }
}
