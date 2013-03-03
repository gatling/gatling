package com.excilys.ebi.gatling.core

package object validation {

	implicit class SuccessWrapper[T](value: T) {
		def success: Validation[T] = Success(value)
	}

	implicit class FailureWrapper(message: String) {
		def failure[T]: Validation[T] = Failure(message)
	}

	implicit class ValidationList[T](validations: List[Validation[T]]) {
		def sequence: Validation[List[T]] = {

			def sequenceRec(validations: List[Validation[T]]): Validation[List[T]] = validations match {
				case Nil => List.empty[T].success
				case head :: tail => head match {
					case Success(entry) => sequenceRec(tail).map(entry :: _)
					case Failure(message) => message.failure
				}
			}
			sequenceRec(validations)

		}
	}

}