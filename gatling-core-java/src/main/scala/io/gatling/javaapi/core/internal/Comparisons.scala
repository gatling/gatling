/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.core.internal

import java.{ lang => jl }

import io.gatling.commons.util.Equality

object Comparisons {
  def equality[T](clazz: Class[_]): Equality[T] =
    if (clazz == classOf[jl.Integer]) {
      Equality.IntEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[String]) {
      Equality.StringEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Int]]) {
      Equality.IntArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Long]]) {
      Equality.LongArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Short]]) {
      Equality.ShortArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Char]]) {
      Equality.CharArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Byte]]) {
      Equality.ByteArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Boolean]]) {
      Equality.BooleanArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Double]]) {
      Equality.DoubleArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Float]]) {
      Equality.FloatArrayEquality.asInstanceOf[Equality[T]]
    } else if (clazz == classOf[Array[Object]]) {
      Equality.ObjectArrayEquality.asInstanceOf[Equality[T]]
    } else {
      Equality.default
    }

  def ordering[T](clazz: Class[_]): Ordering[T] =
    if (clazz == classOf[jl.Integer]) {
      Ordering.Int.asInstanceOf[Ordering[T]]
    } else if (clazz == classOf[jl.Long]) {
      Ordering.Long.asInstanceOf[Ordering[T]]
    } else if (clazz == classOf[jl.Double]) {
      Ordering.Double.IeeeOrdering.asInstanceOf[Ordering[T]]
    } else if (clazz == classOf[String]) {
      Ordering.String.asInstanceOf[Ordering[T]]
    } else {
      throw new IllegalArgumentException(s"No ordering for class $clazz")
    }
}
