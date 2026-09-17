/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import java.{ lang => jl, util => ju }

import scala.collection.mutable

import io.gatling.commons.validation._

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class TypeHelperSpec extends AnyFlatSpecLike with Matchers {
  "asValidation" should "return the string representation of the object when asking for type String" in {
    TypeHelper.validate[String](3.2) shouldBe Success("3.2")
  }

  it should "be able to assign a java Boolean to either a java Boolean or a Scala Boolean" in {
    TypeHelper.validate[jl.Boolean](true: jl.Boolean) shouldBe Success(true: jl.Boolean)
    TypeHelper.validate[Boolean](true: jl.Boolean) shouldBe Success(true)
  }

  it should "be able to assign a java Byte to either a java Byte or a Scala Byte" in {
    TypeHelper.validate[jl.Byte](1.toByte: jl.Byte) shouldBe Success(1.toByte: jl.Byte)
    TypeHelper.validate[Byte](1.toByte: jl.Byte) shouldBe Success(1.toByte)
  }

  it should "be able to assign a java Short to either a java Short or a Scala Short" in {
    TypeHelper.validate[jl.Short](1.toShort: jl.Short) shouldBe Success(1.toShort: jl.Short)
    TypeHelper.validate[Short](1.toShort: jl.Short) shouldBe Success(1.toShort)
  }

  it should "be able to assign a java Integer to either a java Integer or a Scala Int" in {
    TypeHelper.validate[jl.Integer](1: jl.Integer) shouldBe Success(1: jl.Integer)
    TypeHelper.validate[Int](1: jl.Integer) shouldBe Success(1)
  }

  it should "be able to assign a java Long to either a java Long or a Scala Long" in {
    TypeHelper.validate[jl.Long](1L: jl.Long) shouldBe Success(1L: jl.Long)
    TypeHelper.validate[Long](1L: jl.Long) shouldBe Success(1L)
  }

  it should "be able to assign a java Float to either a java Float or a Scala Float" in {
    TypeHelper.validate[jl.Float](1f: jl.Float) shouldBe Success(1f: jl.Float)
    TypeHelper.validate[Float](1f: jl.Float) shouldBe Success(1f)
  }

  it should "be able to assign a java Double to either a java Double or a Scala Double" in {
    TypeHelper.validate[jl.Double](1.0: jl.Double) shouldBe Success(1.0: jl.Double)
    TypeHelper.validate[Double](1.0: jl.Double) shouldBe Success(1.0)
  }

  it should "be able to assign a java Character to either a java Character or a Scala Char" in {
    TypeHelper.validate[jl.Character]('c': jl.Character) shouldBe Success('c': jl.Character)
    TypeHelper.validate[Char]('c': jl.Character) shouldBe Success('c')
  }

  it should "be able to assign a java List to a Seq" in {
    TypeHelper.validate[Seq[Any]](ju.Arrays.asList(1, 2, 3)) shouldBe Success(Seq(1, 2, 3))
  }

  it should "be able to assign a Scala Seq to a Seq" in {
    TypeHelper.validate[Seq[Any]](Seq(1, 2, 3)) shouldBe Success(Seq(1, 2, 3))
    TypeHelper.validate[Seq[Any]](mutable.ArrayBuffer(1, 2, 3)) shouldBe Success(Seq(1, 2, 3))
  }

  it should "be able to assign an Array of objects to a Seq" in {
    TypeHelper.validate[Seq[Any]](Array("foo", "bar")) shouldBe Success(Seq("foo", "bar"))
  }

  it should "be able to assign an Array of primitives to a Seq" in {
    TypeHelper.validate[Seq[Any]](Array(1, 2, 3)) shouldBe Success(Seq(1, 2, 3))
  }

  it should "return a Failure when types are incompatible" in {
    TypeHelper.validate[Int]("foo") shouldBe a[Failure]
  }

  it should "return a NullValueFailure when null" in {
    TypeHelper.validate[String](null: String) shouldBe a[Failure]
  }
}
