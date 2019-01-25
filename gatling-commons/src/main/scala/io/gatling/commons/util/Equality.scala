/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

import java.util.{ Arrays => JArrays }

trait LowPriorityEqualityImplicits {

  implicit def default[T]: Equality[T] = (left: T, right: T) => left == right
  //implicit val AnyArrayEquality: Equality[Array[Any]] = (left: Array[Any], right: Array[Any]) => JArrays.equals(left, right)
}

object Equality extends LowPriorityEqualityImplicits {

  implicit val LongArrayEquality: Equality[Array[Long]] = (left: Array[Long], right: Array[Long]) => JArrays.equals(left, right)
  implicit val IntArrayEquality: Equality[Array[Int]] = (left: Array[Int], right: Array[Int]) => JArrays.equals(left, right)
  implicit val ShortArrayEquality: Equality[Array[Short]] = (left: Array[Short], right: Array[Short]) => JArrays.equals(left, right)
  implicit val CharArrayEquality: Equality[Array[Char]] = (left: Array[Char], right: Array[Char]) => JArrays.equals(left, right)
  implicit val ByteArrayEquality: Equality[Array[Byte]] = (left: Array[Byte], right: Array[Byte]) => JArrays.equals(left, right)
  implicit val BooleanArrayEquality: Equality[Array[Boolean]] = (left: Array[Boolean], right: Array[Boolean]) => JArrays.equals(left, right)
  implicit val DoubleArrayEquality: Equality[Array[Double]] = (left: Array[Double], right: Array[Double]) => JArrays.equals(left, right)
  implicit val FloatArrayEquality: Equality[Array[Float]] = (left: Array[Float], right: Array[Float]) => JArrays.equals(left, right)
  implicit val ObjectArrayEquality: Equality[Array[Object]] = (left: Array[Object], right: Array[Object]) => JArrays.equals(left, right)
}

trait Equality[T] {

  def equals(left: T, right: T): Boolean
}
