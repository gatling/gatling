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

package io.gatling.commons.util

import java.{ time => jt, util => ju }

import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.jdk.DurationConverters._
import scala.reflect.ClassTag
import scala.util.control.NonFatal

import io.gatling.commons.NotNothing
import io.gatling.commons.util.Throwables._
import io.gatling.commons.validation._

trait TypeCaster[T] {

  protected def cceMessage(key: String, value: Any, clazz: Class[_]): String =
    if (key == null) {
      s"Can't cast '$value' of type ${value.getClass} into $clazz"
    } else {
      s"Can't cast attribute '$key' '$value' of type ${value.getClass} into $clazz"
    }

  @throws[ClassCastException]
  def cast(key: String, value: Any): T

  @throws[ClassCastException]
  def cast(value: Any): T = cast(null, value)

  def validate(key: String, value: Any): Validation[T]

  def validate(value: Any): Validation[T] = validate(null, value)
}

trait LowPriorityTypeCaster {

  implicit def genericTypeCaster[T: ClassTag]: TypeCaster[T] = new TypeCaster[T] {

    override def cast(key: String, value: Any): T = {
      val valueClass = value.getClass
      val targetClass = implicitly[ClassTag[T]].runtimeClass
      if (targetClass.isAssignableFrom(valueClass))
        value.asInstanceOf[T]
      else
        throw new ClassCastException(cceMessage(key, value, targetClass))
    }

    override def validate(key: String, value: Any): Validation[T] = {
      val valueClass = value.getClass
      val targetClass = implicitly[ClassTag[T]].runtimeClass
      if (targetClass.isAssignableFrom(valueClass))
        value.asInstanceOf[T].success
      else
        cceMessage(key, value, targetClass).failure
    }
  }
}

object TypeCaster extends LowPriorityTypeCaster {

  private def parseErrorMessage(key: String, value: String, clazz: Class[_], t: Throwable): String =
    if (key == null) {
      s"Can't parse '$value' into $clazz: ${t.detailedMessage}"
    } else {
      s"Can't parse '$key' '$value' into $clazz: ${t.detailedMessage}"
    }

  private def tryParse[T](key: String, value: String, clazz: Class[_])(f: String => T): T =
    try {
      f(value)
    } catch {
      case NonFatal(t) => throw new IllegalArgumentException(parseErrorMessage(key, value, clazz, t))
    }

  private def tryParseV[T](key: String, value: String, clazz: Class[_])(f: String => T): Validation[T] =
    try {
      f(value).success
    } catch {
      case NonFatal(t) => parseErrorMessage(key, value, clazz, t).failure
    }

  implicit val BooleanCaster: TypeCaster[Boolean] = new TypeCaster[Boolean] {

    @throws[ClassCastException]
    override def cast(key: String, value: Any): Boolean =
      value match {
        case v: Boolean => v
        case s: String  => tryParse(key, s, classOf[Boolean])(_.toBoolean)
        case _          => throw new ClassCastException(cceMessage(key, value, classOf[Boolean]))
      }

    override def validate(key: String, value: Any): Validation[Boolean] =
      value match {
        case v: Boolean => if (v) Validation.TrueSuccess else Validation.FalseSuccess
        case s: String  => tryParseV(key, s, classOf[Boolean])(_.toBoolean)
        case _          => cceMessage(key, value, classOf[Boolean]).failure
      }
  }

  implicit val ByteCaster: TypeCaster[Byte] = new TypeCaster[Byte] {

    @throws[ClassCastException]
    override def cast(key: String, value: Any): Byte =
      value match {
        case v: Byte   => v
        case s: String => tryParse(key, s, classOf[Byte])(_.toByte)
        case _         => throw new ClassCastException(cceMessage(key, value, classOf[Byte]))
      }

    override def validate(key: String, value: Any): Validation[Byte] =
      value match {
        case v: Byte   => v.success
        case s: String => tryParseV(key, s, classOf[Byte])(_.toByte)
        case _         => cceMessage(key, value, classOf[Byte]).failure
      }
  }

  implicit val ShortCaster: TypeCaster[Short] = new TypeCaster[Short] {
    @throws[ClassCastException]
    override def cast(key: String, value: Any): Short =
      value match {
        case v: Short  => v
        case s: String => tryParse(key, s, classOf[Short])(_.toShort)
        case _         => throw new ClassCastException(cceMessage(key, value, classOf[Short]))
      }

    override def validate(key: String, value: Any): Validation[Short] =
      value match {
        case v: Short  => v.success
        case s: String => tryParseV(key, s, classOf[Short])(_.toShort)
        case _         => cceMessage(key, value, classOf[Short]).failure
      }
  }

  implicit val IntCaster: TypeCaster[Int] = new TypeCaster[Int] {
    @throws[ClassCastException]
    override def cast(key: String, value: Any): Int =
      value match {
        case v: Int    => v
        case s: String => tryParse(key, s, classOf[Int])(_.toInt)
        case _         => throw new ClassCastException(cceMessage(key, value, classOf[Int]))
      }

    override def validate(key: String, value: Any): Validation[Int] =
      value match {
        case v: Int    => v.success
        case s: String => tryParseV(key, s, classOf[Int])(_.toInt)
        case _         => cceMessage(key, value, classOf[Int]).failure
      }
  }

  implicit val LongCaster: TypeCaster[Long] = new TypeCaster[Long] {
    @throws[ClassCastException]
    override def cast(key: String, value: Any): Long =
      value match {
        case v: Long   => v
        case s: String => tryParse(key, s, classOf[Long])(_.toLong)
        case _         => throw new ClassCastException(cceMessage(key, value, classOf[Long]))
      }

    override def validate(key: String, value: Any): Validation[Long] =
      value match {
        case v: Long   => v.success
        case s: String => tryParseV(key, s, classOf[Long])(_.toLong)
        case _         => cceMessage(key, value, classOf[Long]).failure
      }
  }

  implicit val FloatCaster: TypeCaster[Float] = new TypeCaster[Float] {
    @throws[ClassCastException]
    override def cast(key: String, value: Any): Float =
      value match {
        case v: Float  => v
        case s: String => tryParse(key, s, classOf[Float])(_.toFloat)
        case _         => throw new ClassCastException(cceMessage(key, value, classOf[Float]))
      }

    override def validate(key: String, value: Any): Validation[Float] =
      value match {
        case v: Float  => v.success
        case s: String => tryParseV(key, s, classOf[Float])(_.toFloat)
        case _         => cceMessage(key, value, classOf[Float]).failure
      }
  }

  implicit val DoubleCaster: TypeCaster[Double] = new TypeCaster[Double] {
    @throws[ClassCastException]
    override def cast(key: String, value: Any): Double =
      value match {
        case v: Double => v
        case s: String => tryParse(key, s, classOf[Double])(_.toDouble)
        case _         => throw new ClassCastException(cceMessage(key, value, classOf[Double]))
      }

    override def validate(key: String, value: Any): Validation[Double] =
      value match {
        case v: Double => v.success
        case s: String => tryParseV(key, s, classOf[Double])(_.toDouble)
        case _         => cceMessage(key, value, classOf[Double]).failure
      }
  }

  implicit val CharCaster: TypeCaster[Char] = new TypeCaster[Char] {
    @throws[ClassCastException]
    override def cast(key: String, value: Any): Char =
      value match {
        case v: Char                    => v
        case v: String if v.length == 1 => v.charAt(0)
        case _                          => throw new ClassCastException(cceMessage(key, value, classOf[Char]))
      }

    override def validate(key: String, value: Any): Validation[Char] =
      value match {
        case v: Char                    => v.success
        case v: String if v.length == 1 => v.charAt(0).success
        case _                          => cceMessage(key, value, classOf[Char]).failure
      }
  }

  implicit val StringCaster: TypeCaster[String] = new TypeCaster[String] {
    override def cast(key: String, value: Any): String = value.toString

    override def validate(key: String, value: Any): Validation[String] =
      value.toString.success
  }

  implicit val FiniteDurationCaster: TypeCaster[FiniteDuration] = new TypeCaster[FiniteDuration] {
    @throws[ClassCastException]
    override def cast(key: String, value: Any): FiniteDuration =
      value match {
        case v: Int            => v.seconds
        case v: Long           => v.seconds
        case s: String         => tryParse(key, s, classOf[Long])(_.toLong.seconds)
        case v: FiniteDuration => v
        case v: jt.Duration    => v.toScala
        case _                 => throw new ClassCastException(cceMessage(key, value, classOf[FiniteDuration]))
      }

    override def validate(key: String, value: Any): Validation[FiniteDuration] =
      value match {
        case v: Int            => v.seconds.success
        case v: Long           => v.seconds.success
        case s: String         => tryParseV(key, s, classOf[Long])(_.toLong.seconds)
        case v: FiniteDuration => v.success
        case v: jt.Duration    => v.toScala.success
        case _                 => cceMessage(key, value, classOf[FiniteDuration]).failure
      }
  }

  implicit val SeqTypeCaster: TypeCaster[Seq[Any]] = new TypeCaster[Seq[Any]] {
    override def cast(key: String, value: Any): Seq[Any] =
      value match {
        case scalaSeq: Seq[_]     => scalaSeq
        case javaList: ju.List[_] => javaList.asScala.toSeq
        case _                    => throw new ClassCastException(cceMessage(key, value, classOf[Seq[_]]))
      }

    override def validate(key: String, value: Any): Validation[Seq[Any]] =
      value match {
        case scalaSeq: Seq[_]     => scalaSeq.success
        case javaList: ju.List[_] => javaList.asScala.toSeq.success
        case _                    => cceMessage(key, value, classOf[Seq[_]]).failure
      }
  }

  implicit val MapTypeCaster: TypeCaster[Map[String, Any]] = new TypeCaster[Map[String, Any]] {
    override def cast(key: String, value: Any): Map[String, Any] =
      value match {
        case scalaMap: Map[_, _]   => scalaMap.asInstanceOf[Map[String, Any]]
        case javaMap: ju.Map[_, _] => javaMap.asScala.asInstanceOf[Map[String, Any]]
        case _                     => throw new ClassCastException(cceMessage(key, value, classOf[Map[_, _]]))
      }

    override def validate(key: String, value: Any): Validation[Map[String, Any]] =
      value match {
        case scalaMap: Map[_, _]   => scalaMap.asInstanceOf[Map[String, Any]].success
        case javaMap: ju.Map[_, _] => javaMap.asScala.asInstanceOf[Map[String, Any]].success
        case _                     => cceMessage(key, value, classOf[Map[_, _]]).failure
      }
  }

  implicit val AnyTypeCaster: TypeCaster[Any] = new TypeCaster[Any] {
    override def cast(key: String, value: Any): Any = value

    override def validate(key: String, value: Any): Validation[Any] =
      value.success
  }
}

object TypeHelper {

  private def nullValueMessage(key: String): String =
    if (key == null) "Value is null" else s"Attribute $key's value is null"

  def isNullValueFailure(failure: Failure): Boolean =
    failure.message.endsWith(" is null")

  def cast[T: TypeCaster: ClassTag: NotNothing](value: Any): T = cast(null, value)

  def cast[T: TypeCaster: ClassTag: NotNothing](key: String, value: Any): T =
    if (value == null) {
      throw new ClassCastException(nullValueMessage(key))
    } else {
      implicitly[TypeCaster[T]].cast(key, value)
    }

  def validate[T: TypeCaster: ClassTag: NotNothing](value: Any): Validation[T] = validate(null, value)

  def validate[T: TypeCaster: ClassTag: NotNothing](key: String, value: Any): Validation[T] =
    if (value == null) {
      nullValueMessage(key).failure
    } else {
      implicitly[TypeCaster[T]].validate(key, value)
    }
}
