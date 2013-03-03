package com.excilys.ebi.gatling.core.validation

sealed trait Validation[+T] {
	def map[A](f: T => A): Validation[A]
	def flatMap[A](f: T => Validation[A]): Validation[A]
	def mapError(f: String => String): Validation[T]
}

case class Success[T](value: T) extends Validation[T] {
	def map[A](f: T => A): Validation[A] = Success(f(value))
	def flatMap[A](f: T => Validation[A]): Validation[A] = f(value)
	def mapError(f: String => String): Validation[T] = this
}

case class Failure[T](message: String) extends Validation[T] {
	def map[A](f: T => A): Validation[A] = Failure(message)
	def flatMap[A](f: T => Validation[A]): Validation[A] = Failure(message)
	def mapError(f: String => String): Validation[T] = Failure(f(message))
}