/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

import scala.reflect.ClassTag
import scala.util.control.NonFatal

import io.gatling.commons.NotNothing
import io.gatling.commons.validation._
import io.gatling.commons.util.Throwables._

trait TypeCaster[T] {

  protected def cceMessage(value: Any, clazz: Class[_]) = s"Can't cast value $value of type ${value.getClass} into $clazz"

  @throws[ClassCastException]
  def cast(value: Any): T

  def validate(value: Any): Validation[T]
}

trait LowPriorityTypeCaster {

  implicit def genericTypeCaster[T: ClassTag] = new TypeCaster[T] {

    @throws[ClassCastException]
    override def cast(value: Any): T = {
      val valueClass = value.getClass
      val targetClass = implicitly[ClassTag[T]].runtimeClass
      if (targetClass.isAssignableFrom(valueClass))
        value.asInstanceOf[T]
      else
        throw new ClassCastException(cceMessage(value, targetClass))
    }

    override def validate(value: Any): Validation[T] = {
      val valueClass = value.getClass
      val targetClass = implicitly[ClassTag[T]].runtimeClass
      if (targetClass.isAssignableFrom(valueClass))
        value.asInstanceOf[T].success
      else
        cceMessage(value, targetClass).failure
    }
  }
}

object TypeCaster extends LowPriorityTypeCaster {

  private def safely[T](f: => T): Validation[T] =
    try { f.success }
    catch {
      case NonFatal(e) =>
        e.detailedMessage.failure
    }

  implicit val BooleanCaster = new TypeCaster[Boolean] {
    @throws[ClassCastException]
    override def cast(value: Any): Boolean =
      value match {
        case v: Boolean           => v
        case v: java.lang.Boolean => v.booleanValue
        case v: String            => v.toBoolean
        case _                    => throw new ClassCastException(cceMessage(value, classOf[Boolean]))
      }

    override def validate(value: Any): Validation[Boolean] =
      value match {
        case true | java.lang.Boolean.TRUE   => TrueSuccess
        case false | java.lang.Boolean.FALSE => FalseSuccess
        case s: String                       => safely(s.toBoolean)
        case _                               => cceMessage(value, classOf[Boolean]).failure
      }
  }

  implicit val ByteCaster = new TypeCaster[Byte] {
    @throws[ClassCastException]
    override def cast(value: Any): Byte =
      value match {
        case v: Byte           => v
        case v: java.lang.Byte => v.byteValue
        case v: String         => v.toByte
        case _                 => throw new ClassCastException(cceMessage(value, classOf[Byte]))
      }

    override def validate(value: Any): Validation[Byte] =
      value match {
        case v: Byte           => v.success
        case v: java.lang.Byte => v.byteValue.success
        case v: String         => safely(v.toByte)
        case _                 => cceMessage(value, classOf[Byte]).failure
      }
  }

  implicit val ShortCaster = new TypeCaster[Short] {
    @throws[ClassCastException]
    override def cast(value: Any): Short =
      value match {
        case v: Short           => v
        case v: java.lang.Short => v.shortValue
        case v: String          => v.toShort
        case _                  => throw new ClassCastException(cceMessage(value, classOf[Short]))
      }

    override def validate(value: Any): Validation[Short] =
      value match {
        case v: Short           => v.success
        case v: java.lang.Short => v.shortValue.success
        case v: String          => safely(v.toShort)
        case _                  => cceMessage(value, classOf[Short]).failure
      }
  }

  implicit val IntCaster = new TypeCaster[Int] {
    @throws[ClassCastException]
    override def cast(value: Any): Int =
      value match {
        case v: Int               => v
        case v: java.lang.Integer => v.intValue
        case v: String            => v.toInt
        case _                    => throw new ClassCastException(cceMessage(value, classOf[Int]))
      }

    override def validate(value: Any): Validation[Int] =
      value match {
        case v: Int               => v.success
        case v: java.lang.Integer => v.intValue.success
        case v: String            => safely(v.toInt)
        case _                    => cceMessage(value, classOf[Int]).failure
      }
  }

  implicit val LongCaster = new TypeCaster[Long] {
    @throws[ClassCastException]
    override def cast(value: Any): Long =
      value match {
        case v: Long           => v
        case v: java.lang.Long => v.longValue
        case v: String         => v.toLong
        case _                 => throw new ClassCastException(cceMessage(value, classOf[Long]))
      }

    override def validate(value: Any): Validation[Long] =
      value match {
        case v: Long           => v.success
        case v: java.lang.Long => v.longValue.success
        case v: String         => safely(v.toLong)
        case _                 => cceMessage(value, classOf[Long]).failure
      }
  }

  implicit val FloatCaster = new TypeCaster[Float] {
    @throws[ClassCastException]
    override def cast(value: Any): Float =
      value match {
        case v: Float           => v
        case v: java.lang.Float => v.floatValue
        case v: String          => v.toFloat
        case _                  => throw new ClassCastException(cceMessage(value, classOf[Float]))
      }

    override def validate(value: Any): Validation[Float] =
      value match {
        case v: Float           => v.success
        case v: java.lang.Float => v.floatValue.success
        case v: String          => safely(v.toFloat)
        case _                  => cceMessage(value, classOf[Float]).failure
      }
  }

  implicit val DoubleCaster = new TypeCaster[Double] {
    @throws[ClassCastException]
    override def cast(value: Any): Double =
      value match {
        case v: Double           => v
        case v: java.lang.Double => v.doubleValue
        case v: String           => v.toDouble
        case _                   => throw new ClassCastException(cceMessage(value, classOf[Double]))
      }

    override def validate(value: Any): Validation[Double] =
      value match {
        case v: Double           => v.success
        case v: java.lang.Double => v.doubleValue.success
        case v: String           => safely(v.toDouble)
        case _                   => cceMessage(value, classOf[Double]).failure
      }
  }

  implicit val CharCaster = new TypeCaster[Char] {
    @throws[ClassCastException]
    override def cast(value: Any): Char =
      value match {
        case v: Char                    => v
        case v: java.lang.Character     => v.charValue
        case v: String if v.length == 1 => v.charAt(0)
        case _                          => throw new ClassCastException(cceMessage(value, classOf[Char]))
      }

    override def validate(value: Any): Validation[Char] =
      value match {
        case v: Char                    => v.success
        case v: java.lang.Character     => v.charValue.success
        case v: String if v.length == 1 => v.charAt(0).success
        case _                          => cceMessage(value, classOf[Char]).failure
      }
  }

  implicit val StringCaster = new TypeCaster[String] {
    override def cast(value: Any): String = value.toString

    override def validate(value: Any): Validation[String] =
      value.toString.success
  }

  implicit val AnyTypeCaster = new TypeCaster[Any] {
    override def cast(value: Any): Any = value

    override def validate(value: Any): Validation[Any] =
      value.success
  }
}

object TypeHelper {

  val NullValueFailure = "Value is null".failure

  implicit class TypeValidator(val value: Any) extends AnyVal {

    def as[T: TypeCaster: ClassTag: NotNothing]: T = Option(value) match {
      case Some(v) => implicitly[TypeCaster[T]].cast(v)
      case _       => throw new ClassCastException(NullValueFailure.message)
    }

    def asOption[T: TypeCaster: ClassTag: NotNothing]: Option[T] = Option(value) match {
      case Some(v) => Some(implicitly[TypeCaster[T]].cast(v))
      case _       => throw new ClassCastException(NullValueFailure.message)
    }

    def asValidation[T: TypeCaster: ClassTag: NotNothing]: Validation[T] = Option(value) match {
      case Some(v) => implicitly[TypeCaster[T]].validate(v)
      case _       => NullValueFailure
    }
  }
}
