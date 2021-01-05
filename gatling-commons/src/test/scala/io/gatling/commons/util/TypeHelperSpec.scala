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

package io.gatling.commons.util

import java.lang

import io.gatling.BaseSpec
import io.gatling.commons.validation._

class TypeHelperSpec extends BaseSpec {

  "asValidation" should "return the string representation of the object when asking for type String" in {
    TypeHelper.validate[String](3.2) shouldBe Success("3.2")
  }

  it should "be able to assign a java Boolean to either a java Boolean or a Scala Boolean" in {
    TypeHelper.validate[lang.Boolean](true: lang.Boolean) shouldBe Success(true: lang.Boolean)
    TypeHelper.validate[Boolean](true: lang.Boolean) shouldBe Success(true)
  }

  it should "be able to assign a java Byte to either a java Byte or a Scala Byte" in {
    TypeHelper.validate[lang.Byte](1.toByte: lang.Byte) shouldBe Success(1.toByte: lang.Byte)
    TypeHelper.validate[Byte](1.toByte: lang.Byte) shouldBe Success(1.toByte)
  }

  it should "be able to assign a java Short to either a java Short or a Scala Short" in {
    TypeHelper.validate[lang.Short](1.toShort: lang.Short) shouldBe Success(1.toShort: lang.Short)
    TypeHelper.validate[Short](1.toShort: lang.Short) shouldBe Success(1.toShort)
  }

  it should "be able to assign a java Integer to either a java Integer or a Scala Int" in {
    TypeHelper.validate[lang.Integer](1: lang.Integer) shouldBe Success(1: lang.Integer)
    TypeHelper.validate[Int](1: lang.Integer) shouldBe Success(1)
  }

  it should "be able to assign a java Long to either a java Long or a Scala Long" in {
    TypeHelper.validate[lang.Long](1L: lang.Long) shouldBe Success(1L: lang.Long)
    TypeHelper.validate[Long](1L: lang.Long) shouldBe Success(1L)
  }

  it should "be able to assign a java Float to either a java Float or a Scala Float" in {
    TypeHelper.validate[lang.Float](1f: lang.Float) shouldBe Success(1f: lang.Float)
    TypeHelper.validate[Float](1f: lang.Float) shouldBe Success(1f)
  }

  it should "be able to assign a java Double to either a java Double or a Scala Double" in {
    TypeHelper.validate[lang.Double](1.0: lang.Double) shouldBe Success(1.0: lang.Double)
    TypeHelper.validate[Double](1.0: lang.Double) shouldBe Success(1.0)
  }

  it should "be able to assign a java Character to either a java Character or a Scala Char" in {
    TypeHelper.validate[lang.Character]('c': lang.Character) shouldBe Success('c': lang.Character)
    TypeHelper.validate[Char]('c': lang.Character) shouldBe Success('c')
  }

  it should "return a Failure when types are incompatible" in {
    TypeHelper.validate[Int]("foo") shouldBe a[Failure]
  }

  it should "return a NullValueFailure when null" in {
    TypeHelper.validate[String](null: String) shouldBe a[Failure]
  }
}
