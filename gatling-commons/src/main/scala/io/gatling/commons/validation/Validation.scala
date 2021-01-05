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

package io.gatling.commons.validation

object Validation {
  val unit: Validation[Unit] = ().success
}

sealed trait Validation[@specialized(Short, Int, Long, Double, Char, Boolean) +T] {
  def map[A](f: T => A): Validation[A]
  def flatMap[A](f: T => Validation[A]): Validation[A]
  def mapError(f: String => String): Validation[T]
  def foreach(f: T => Any): Unit = onSuccess(f)
  def withFilter(p: T => Boolean): Validation[T] = filter(p)
  def filter(p: T => Boolean): Validation[T]
  def onSuccess(f: T => Any): Unit
  def onFailure(f: String => Any): Unit
  def recover[A >: T](v: => A): Validation[A]
  def toOption: Option[T]
}

final case class Success[+T](value: T) extends Validation[T] {
  override def map[A](f: T => A): Validation[A] = Success(f(value))
  override def flatMap[A](f: T => Validation[A]): Validation[A] = f(value)
  override def mapError(f: String => String): Validation[T] = this
  override def filter(p: T => Boolean): Validation[T] = if (p(value)) this else Failure("Predicate does not hold for " + value.toString)
  override def onSuccess(f: T => Any): Unit = f(value)
  override def onFailure(f: String => Any): Unit = ()
  override def recover[A >: T](v: => A): Validation[A] = this
  override def toOption: Option[T] = Some(value)
}

final case class Failure(message: String) extends Validation[Nothing] {
  override def map[A](f: Nothing => A): Validation[A] = this
  override def flatMap[A](f: Nothing => Validation[A]): Validation[A] = this
  override def mapError(f: String => String): Validation[Nothing] = Failure(f(message))
  override def filter(p: Nothing => Boolean): Failure = this
  override def onSuccess(f: Nothing => Any): Unit = ()
  override def onFailure(f: String => Any): Unit = f(message)
  override def recover[A >: Nothing](v: => A): Validation[A] = v.success
  override def toOption: Option[Nothing] = None
}
