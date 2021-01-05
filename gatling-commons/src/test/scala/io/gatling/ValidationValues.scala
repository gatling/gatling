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

package io.gatling

import io.gatling.commons.validation._

import org.scalatest.exceptions.TestFailedException

trait ValidationValues {

  implicit def validation2ValidationValuable[T](validation: Validation[T]): ValidationValuable[T] = new ValidationValuable[T](validation)

  class ValidationValuable[T](validation: Validation[T]) {

    def succeeded: T = validation match {
      case Success(v) => v
      case Failure(msg) =>
        throw new TestFailedException(s"Cannot call .value on $validation, was a Failure", 0)
    }

    def failed: String = validation match {
      case Success(v) =>
        throw new TestFailedException(s"Cannot call .failMessage on $validation, was a Success", 0)
      case Failure(msg) => msg
    }
  }
}
