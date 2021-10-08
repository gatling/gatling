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

package io.gatling.javaapi.core.internal

import java.{ lang => jl, util => ju }
import java.time.Duration
import java.util.{ function => juf }
import java.util.concurrent.TimeUnit

import scala.collection.immutable.Seq
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._
import scala.jdk.DurationConverters._

import io.gatling.commons.validation._
import io.gatling.core.session.{ Session => ScalaSession, _ }
import io.gatling.core.session.el.El
import io.gatling.core.structure.Pauses
import io.gatling.javaapi.core.Session
import io.gatling.javaapi.core.internal.Converters.toScalaTuple2Seq

object Expressions {

  def expressionToJavaFunction[T](f: Expression[T]): JavaExpression[T] =
    session =>
      f(session.asScala()) match {
        case Success(value)   => value
        case Failure(message) => throw new RuntimeException(message)
      }

  /**
   * Typically used when Scala method expects types Java can express, eg String
   */
  def javaFunctionToExpression[T](f: JavaExpression[T]): Expression[T] =
    session => safely()(f.apply(new Session(session)).success)

  def javaBiFunctionToExpression[U, R](f: juf.BiFunction[U, Session, R]): (U, ScalaSession) => Validation[R] =
    (u, session) => safely()(f.apply(u, new Session(session)).success)

  def toStaticValueExpression[T](value: T): Expression[T] =
    value.expressionSuccess

  def javaLongFunctionToExpression(f: JavaExpression[jl.Long]): Expression[Long] =
    session => safely()(f.apply(new Session(session)).longValue.success)

  def javaIntegerFunctionToExpression(f: JavaExpression[jl.Integer]): Expression[Int] =
    session => safely()(f.apply(new Session(session)).intValue.success)

  def javaBooleanFunctionToExpression(f: JavaExpression[jl.Boolean]): Expression[Boolean] =
    session => safely()(f.apply(new Session(session)).booleanValue.success)

  def javaObjectFunctionToExpression(f: JavaExpression[jl.Object]): Expression[Any] =
    session => safely()(f.apply(new Session(session)).success)

  def javaDurationFunctionToExpression(f: JavaExpression[Duration]): Expression[FiniteDuration] =
    session => safely()(f.apply(new Session(session)).toScala.success)

  def javaListFunctionToImmutableSeqExpression[T](f: JavaExpression[ju.List[T]]): Expression[collection.immutable.Seq[T]] =
    session => safely()(f.apply(new Session(session)).asScala.toSeq.success)

  def javaMapFunctionToImmutableMapExpression[K, V](f: JavaExpression[ju.Map[K, V]]): Expression[collection.immutable.Map[K, V]] =
    session => safely()(f.apply(new Session(session)).asScala.toMap.success)

  def javaPairListFunctionToTuple2SeqExpression(f: JavaExpression[ju.List[ju.Map.Entry[String, Object]]]): Expression[Seq[(String, Object)]] =
    session =>
      safely() {
        val seq = f.apply(new Session(session))
        toScalaTuple2Seq(seq).success
      }

  def toAnyExpression(s: String): Expression[Any] =
    s.el[Any]

  def toStringExpression(s: String): Expression[String] =
    s.el[String]

  def toIntExpression(s: String): Expression[Int] =
    s.el[Int]

  def toBooleanExpression(s: String): Expression[Boolean] =
    s.el[Boolean]

  def toBytesExpression(s: String): Expression[Array[Byte]] =
    s.el[Array[Byte]]

  def toDurationExpression(s: String): Expression[FiniteDuration] =
    Pauses.durationExpression(s, Some(TimeUnit.SECONDS))

  def toSeqExpression[T](s: String): Expression[Seq[T]] =
    s.el[Seq[T]]

  def toMapExpression(s: String): Expression[Map[String, Any]] =
    s.el[Map[String, Any]]

  // TODO List and Map
  def toExpression[T](s: String, clazz: Class[_]): Expression[T] = {
    val expression =
      if (clazz == classOf[String]) {
        s.el[String]
      } else if (clazz == classOf[jl.Boolean]) {
        s.el[Boolean]
      } else if (clazz == classOf[jl.Integer]) {
        s.el[Int]
      } else if (clazz == classOf[jl.Long]) {
        s.el[Long]
      } else if (clazz == classOf[jl.Float]) {
        s.el[Float]
      } else if (clazz == classOf[jl.Double]) {
        s.el[Double]
      } else if (clazz == classOf[Array[Byte]]) {
        s.el[Array[Byte]]
      } else {
        s.el[Any]
      }
    expression.asInstanceOf[Expression[T]]
  }

  def validation[T](value: () => T): Validation[T] =
    safely(identity)(Success(value.apply()))
}
