/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import java.{ util => ju }

trait LowPriorityEqualityImplicits {
  implicit def default[T]: Equality[T] = _ == _
}

object Equality extends LowPriorityEqualityImplicits {
  implicit val IntEquality: Equality[Int] = _ == _
  implicit val StringEquality: Equality[String] = _ == _
  implicit val LongArrayEquality: Equality[Array[Long]] = ju.Arrays.equals(_, _)
  implicit val IntArrayEquality: Equality[Array[Int]] = ju.Arrays.equals(_, _)
  implicit val ShortArrayEquality: Equality[Array[Short]] = ju.Arrays.equals(_, _)
  implicit val CharArrayEquality: Equality[Array[Char]] = ju.Arrays.equals(_, _)
  implicit val ByteArrayEquality: Equality[Array[Byte]] = ju.Arrays.equals(_, _)
  implicit val BooleanArrayEquality: Equality[Array[Boolean]] = ju.Arrays.equals(_, _)
  implicit val DoubleArrayEquality: Equality[Array[Double]] = ju.Arrays.equals(_, _)
  implicit val FloatArrayEquality: Equality[Array[Float]] = ju.Arrays.equals(_, _)
  implicit val ObjectArrayEquality: Equality[Array[Object]] = ju.Arrays.equals(_, _)
}

trait Equality[T] {
  def equals(left: T, right: T): Boolean
}
