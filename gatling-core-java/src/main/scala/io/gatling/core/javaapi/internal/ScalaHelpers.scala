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

package io.gatling.core.javaapi.internal

import java.{ lang => jl, util => ju }
import java.time.Duration
import java.util.{ function => juf }
import java.util.concurrent.TimeUnit

import scala.collection.immutable.{ ArraySeq, Seq }
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._
import scala.jdk.DurationConverters._
import scala.jdk.FunctionConverters._

import io.gatling.commons.util.Equality
import io.gatling.commons.validation._
import io.gatling.core.body.{ Body => ScalaBody }
import io.gatling.core.check.CheckBuilder
import io.gatling.core.feeder.Feeder
import io.gatling.core.javaapi.{ Body, Session }
import io.gatling.core.session.{ Expression, StaticValueExpression, Session => ScalaSession }
import io.gatling.core.session.el.El
import io.gatling.core.structure.Pauses

object ScalaHelpers {

  def toGatlingSessionToSessionFunction(f: juf.Function[Session, Session]): Expression[ScalaSession] =
    session => safely()(f.apply(new Session(session)).asScala().success)

  def toJavaFunction[T](f: Expression[T]): juf.Function[Session, T] =
    session =>
      f(session.asScala()) match {
        case Success(value)   => value
        case Failure(message) => throw new RuntimeException(message)
      }

  /**
   * Typically used when Scala method expects types Java can express, eg String
   */
  def toTypedGatlingSessionFunction[T](f: juf.Function[Session, T]): Expression[T] =
    session => safely()(f.apply(new Session(session)).success)

  def toTypedGatlingSessionFunction[U, R](f: juf.BiFunction[U, Session, R]): (U, ScalaSession) => Validation[R] =
    (u, session) => safely()(f.apply(u, new Session(session)).success)

  def toStaticValueExpression[T](value: T): Expression[T] =
    StaticValueExpression(value)

  def toUntypedGatlingSessionFunction[T](f: juf.Function[Session, T]): Expression[Object] =
    session => safely()(f.apply(new Session(session)).success.asInstanceOf[Validation[Object]])

  def toUntypedGatlingSessionFunction[U, R](f: juf.BiFunction[U, Session, R]): (U, ScalaSession) => Validation[Object] =
    (u, session) => safely()(f.apply(u, new Session(session)).success.asInstanceOf[Validation[Object]])

  def toGatlingSessionFunctionLong(f: juf.Function[Session, jl.Long]): Expression[Long] =
    session => safely()(f.apply(new Session(session)).longValue.success)

  def toGatlingSessionFunctionDuration(f: juf.Function[Session, Duration]): Expression[FiniteDuration] =
    session => safely()(f.apply(new Session(session)).toScala.success)

  def toGatlingSessionFunctionImmutableSeq[T](f: juf.Function[Session, ju.List[T]]): Expression[collection.immutable.Seq[T]] =
    session => safely()(f.apply(new Session(session)).asScala.toSeq.success)

  def toGatlingSessionFunctionImmutableMap[K, V](f: juf.Function[Session, ju.Map[K, V]]): Expression[collection.immutable.Map[K, V]] =
    session => safely()(f.apply(new Session(session)).asScala.toMap.success)

  def toGatlingSessionFunctionTuple2Seq(f: juf.Function[Session, ju.List[ju.Map.Entry[String, Object]]]): Expression[Seq[(String, Object)]] =
    session =>
      safely() {
        val seq = f.apply(new Session(session))
        toScalaTuple2Seq(seq).success
      }

  def toScalaFunction[T, R](f: juf.Function[T, R]): Function[T, R] = f.asScala

  def toScalaDuration(duration: Duration): FiniteDuration = duration.toScala

  def toScalaSeq[T](array: Array[T]): Seq[T] = ArraySeq.unsafeWrapArray(array)

  def toScalaSeq[T](list: ju.List[T]): Seq[T] = list.asScala.toSeq

  def toJavaList[T](seq: scala.collection.Seq[T]): ju.List[T] = new ju.ArrayList(seq.asJavaCollection)

  def toScalaSet[T](set: ju.Set[T]): Set[T] = set.asScala.toSet

  def toJavaSet(set: scala.collection.Set[_]): ju.Set[Object] = set.asJava.asInstanceOf[ju.Set[Object]]

  def toJavaMap(map: scala.collection.Map[String, Object]): ju.Map[String, Object] = map.asJava

  def toScalaMap[K, V](list: ju.Map[K, V]): Map[K, V] = list.asScala.toMap

  def toScalaTuple2Seq(list: ju.List[ju.Map.Entry[String, Object]]): Seq[(String, Object)] =
    list.asScala.map(entry => entry.getKey -> entry.getValue).toSeq

  def toScalaFeeder(it: ju.Iterator[ju.Map[String, Object]]): Feeder[Any] =
    it.asScala.map(_.asScala.toMap)

  def toScalaFeeder(sup: juf.Supplier[ju.Map[String, Object]]): Feeder[Any] =
    Iterator.continually(sup.get().asScala.toMap)

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

  def equality[T](clazz: Class[_]): Equality[T] =
    if (clazz == classOf[jl.Integer]) {
      Equality.IntEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[String]) {
      Equality.StringEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Int]]) {
      Equality.IntArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Long]]) {
      Equality.LongArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Short]]) {
      Equality.ShortArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Char]]) {
      Equality.CharArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Byte]]) {
      Equality.ByteArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Boolean]]) {
      Equality.BooleanArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Double]]) {
      Equality.DoubleArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Float]]) {
      Equality.FloatArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Object]]) {
      Equality.ObjectArrayEquality.asInstanceOf[Equality[T]]
    } else {
      Equality.default
    }

  def ordering[T](clazz: Class[_]): Ordering[T] =
    if (clazz == classOf[jl.Integer]) {
      Ordering.Int.asInstanceOf[Ordering[T]]
    } else if (clazz == classOf[jl.Long]) {
      Ordering.Long.asInstanceOf[Ordering[T]]
    } else if (clazz == classOf[jl.Double]) {
      Ordering.Double.asInstanceOf[Ordering[T]]
    } else if (clazz == classOf[String]) {
      Ordering.String.asInstanceOf[Ordering[T]]
    } else {
      throw new IllegalArgumentException(s"No ordering for class $clazz")
    }

  def transformSingleCheck[T, P, ScalaX, JavaX](
      wrapped: CheckBuilder.Validate[T, P, ScalaX],
      scalaXToJavaX: juf.Function[ScalaX, JavaX]
  ): CheckBuilder.Validate[T, P, JavaX] =
    wrapped.transform(scalaXToJavaX.asScala)

  def transformSeqCheck[T, P, ScalaX, JavaX](
      wrapped: CheckBuilder.Validate[T, P, Seq[ScalaX]],
      scalaXToJavaX: juf.Function[ScalaX, JavaX]
  ): CheckBuilder.Validate[T, P, ju.List[JavaX]] =
    wrapped.transform(_.map(scalaXToJavaX.asScala).asJava)

  def toFindRandomCheck[T, P, X](wrapped: CheckBuilder.MultipleFind[T, P, X], num: Int, failIfLess: Boolean): CheckBuilder.Validate[T, P, ju.List[X]] =
    wrapped.findRandom(num, failIfLess).transform(_.asJava)

  def toCountCheck[T, P, X](wrapped: CheckBuilder.MultipleFind[T, P, X]): CheckBuilder.Validate[T, P, jl.Integer] =
    wrapped.count.transform(_.asInstanceOf[jl.Integer])

  def toJavaBody(scalaBody: ScalaBody): Body = scalaBody match {
    case b: io.gatling.core.body.StringBody      => new Body.WithString(b)
    case b: io.gatling.core.body.RawFileBody     => new Body.WithBytes(b)
    case b: io.gatling.core.body.ByteArrayBody   => new Body.WithBytes(b)
    case b: io.gatling.core.body.ElBody          => new Body.WithString(b)
    case b: io.gatling.core.body.InputStreamBody => new Body.Default(b)
  }
}
