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
package io.gatling.commons

import scala.util.control.NonFatal

import com.typesafe.scalalogging.StrictLogging

package object validation extends StrictLogging {

  val TrueSuccess = true.success
  val FalseSuccess = false.success
  val NoneSuccess = None.success
  val NullStringSuccess = "null".success

  def safely[T](errorMapper: String => String = identity)(f: => Validation[T]): Validation[T] =
    try { f }
    catch {
      case NonFatal(e) =>
        val message = errorMapper(e.getClass.getSimpleName + ": " + e.getMessage)
        logger.info(message, e)
        message.failure
    }

  implicit class SuccessWrapper[T](val value: T) extends AnyVal {
    def success: Validation[T] = Success(value)
  }

  implicit class FailureWrapper(val message: String) extends AnyVal {
    def failure = Failure(message)
  }
}
