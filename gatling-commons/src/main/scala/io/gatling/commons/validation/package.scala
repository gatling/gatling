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

package io.gatling.commons

import scala.util.{ Try, Failure => TFailure, Success => TSuccess }
import scala.util.control.NonFatal

import io.gatling.commons.util.Throwables._

import com.typesafe.scalalogging.StrictLogging

package object validation extends StrictLogging {

  val TrueSuccess: Validation[Boolean] = true.success
  val FalseSuccess: Validation[Boolean] = false.success
  val NoneSuccess: Validation[None.type] = None.success
  val NullStringSuccess: Validation[String] = "null".success

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def safely[T](errorMapper: String => String = identity)(f: => Validation[T]): Validation[T] =
    try {
      f
    } catch {
      case NonFatal(e) =>
        val message = errorMapper(e.detailedMessage)
        logger.debug(message, e)
        message.failure
    }

  implicit class SuccessWrapper[T](val value: T) extends AnyVal {
    def success: Validation[T] = Success(value)
  }

  implicit class FailureWrapper(val message: String) extends AnyVal {
    def failure: Failure = Failure(message)
  }

  implicit class OptionWrapper[T](val option: Option[T]) extends AnyVal {
    def toValidation(error: => String): Validation[T] = option match {
      case Some(value) => Success(value)
      case _           => Failure(error)
    }
  }

  implicit class TryWrapper[T](val t: Try[T]) extends AnyVal {
    def toValidation: Validation[T] = t match {
      case TSuccess(value) => Success(value)
      case TFailure(e)     => Failure(e.rootMessage)
    }
  }
}
