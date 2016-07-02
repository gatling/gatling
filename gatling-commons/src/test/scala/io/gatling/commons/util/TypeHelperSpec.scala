/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
import io.gatling.commons.util.TypeHelper._
import io.gatling.commons.validation._

class TypeHelperSpec extends BaseSpec {

  "asValidation" should "return the string representation of the object when asking for type String" in {
    3.2.asValidation[String] shouldBe Success("3.2")
  }

  it should "be able to assign a java Boolean to either a java Boolean or a Scala Boolean" in {
    (true: lang.Boolean).asValidation[lang.Boolean] shouldBe Success(true: lang.Boolean)
    (true: lang.Boolean).asValidation[Boolean] shouldBe Success(true)
  }

  it should "be able to assign a java Byte to either a java Byte or a Scala Byte" in {
    (1.toByte: lang.Byte).asValidation[lang.Byte] shouldBe Success(1.toByte: lang.Byte)
    (1.toByte: lang.Byte).asValidation[Byte] shouldBe Success(1.toByte)
  }

  it should "be able to assign a java Short to either a java Short or a Scala Short" in {
    (1.toShort: lang.Short).asValidation[lang.Short] shouldBe Success(1.toShort: lang.Short)
    (1.toShort: lang.Short).asValidation[Short] shouldBe Success(1.toShort)
  }

  it should "be able to assign a java Integer to either a java Integer or a Scala Int" in {
    (1: lang.Integer).asValidation[lang.Integer] shouldBe Success(1: lang.Integer)
    (1: lang.Integer).asValidation[Int] shouldBe Success(1)
  }

  it should "be able to assign a java Long to either a java Long or a Scala Long" in {
    (1L: lang.Long).asValidation[lang.Long] shouldBe Success(1L: lang.Long)
    (1L: lang.Long).asValidation[Long] shouldBe Success(1L)
  }

  it should "be able to assign a java Float to either a java Float or a Scala Float" in {
    (1f: lang.Float).asValidation[lang.Float] shouldBe Success(1f: lang.Float)
    (1f: lang.Float).asValidation[Float] shouldBe Success(1f)
  }

  it should "be able to assign a java Double to either a java Double or a Scala Double" in {
    (1.0: lang.Double).asValidation[lang.Double] shouldBe Success(1.0: lang.Double)
    (1.0: lang.Double).asValidation[Double] shouldBe Success(1.0)
  }

  it should "be able to assign a java Character to either a java Character or a Scala Char" in {
    ('c': lang.Character).asValidation[lang.Character] shouldBe Success('c': lang.Character)
    ('c': lang.Character).asValidation[Char] shouldBe Success('c')
  }

  it should "return a Failure when types are incompatible" in {
    "foo".asValidation[Int] shouldBe a[Failure]
  }

  it should "return a NullValueFailure when null" in {
    (null: String).asValidation[String] shouldBe NullValueFailure
  }
}
