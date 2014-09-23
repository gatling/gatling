package io.gatling.core.test

import org.scalatest.exceptions.TestFailedException

import io.gatling.core.validation._

trait ValidationValues {

  implicit def validation2ValidationValuable[T](validation: Validation[T]) = new ValidationValuable[T](validation)

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
