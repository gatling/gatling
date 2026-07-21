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

package io.gatling.core.json

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class JsonValidatorSpec extends AnyFlatSpecLike with Matchers {

  "isValid" should "accept a JSON object" in {
    JsonValidator.isValid("{}") shouldBe true
    JsonValidator.isValid("""{"key":"value"}""") shouldBe true
    JsonValidator.isValid("""{"a":1,"b":true,"c":null}""") shouldBe true
  }

  it should "accept a JSON array" in {
    JsonValidator.isValid("[]") shouldBe true
    JsonValidator.isValid("[1,2,3]") shouldBe true
    JsonValidator.isValid("""["foo","bar"]""") shouldBe true
  }

  it should "accept JSON primitives" in {
    JsonValidator.isValid(""""hello"""") shouldBe true
    JsonValidator.isValid("42") shouldBe true
    JsonValidator.isValid("-3.14") shouldBe true
    JsonValidator.isValid("1e10") shouldBe true
    JsonValidator.isValid("true") shouldBe true
    JsonValidator.isValid("false") shouldBe true
    JsonValidator.isValid("null") shouldBe true
  }

  it should "accept nested structures" in {
    JsonValidator.isValid("""{"a":{"b":[1,2]}}""") shouldBe true
    JsonValidator.isValid("""[{"x":1},{"x":2}]""") shouldBe true
  }

  it should "accept values with surrounding whitespace" in {
    JsonValidator.isValid("  { }  ") shouldBe true
    JsonValidator.isValid(" [ 1 , 2 ] ") shouldBe true
  }

  it should "accept inputs longer than 80 characters (PathLong path)" in {
    val longJson = """{"description":"this is a long string value that exceeds the threshold for the long path in the validator"}"""
    longJson.length should be > 80
    JsonValidator.isValid(longJson) shouldBe true
  }

  it should "reject null input" in {
    JsonValidator.isValid(null) shouldBe false
  }

  it should "reject an empty string" in {
    JsonValidator.isValid("") shouldBe false
  }

  it should "reject a bare unquoted word" in {
    JsonValidator.isValid("hello") shouldBe false
  }

  it should "reject an object with unquoted keys" in {
    JsonValidator.isValid("{invalid}") shouldBe false
    JsonValidator.isValid("{key:1}") shouldBe false
  }

  it should "reject an array with unquoted string elements" in {
    JsonValidator.isValid("[invalid]") shouldBe false
  }

  it should "reject truncated JSON" in {
    JsonValidator.isValid("{") shouldBe false
    JsonValidator.isValid("[") shouldBe false
    JsonValidator.isValid("""{"key":""") shouldBe false
  }

  it should "reject a string with an unterminated escape sequence" in {
    JsonValidator.isValid(""""bad\\""") shouldBe false
  }
}
