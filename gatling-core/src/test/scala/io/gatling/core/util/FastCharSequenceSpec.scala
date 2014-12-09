/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.util

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen.posNum
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{ FlatSpec, Matchers }

class FastCharSequenceSpec extends FlatSpec with Matchers with GeneratorDrivenPropertyChecks {

  "FastCharSequence.Empty" should "have a zero length a be the empty string" in {
    FastCharSequence.Empty.length shouldBe 0
    FastCharSequence.Empty.toString shouldBe ""
  }

  "isBlank" should "return true only if all characters are spaces" in {
    FastCharSequence("            ").isBlank shouldBe true
  }

  it should "return false if at least one character is a not a space" in {
    FastCharSequence("           b").isBlank shouldBe false
  }

  "charAt" should "behave like CharSequence's charAt" in {
    forAll(arbitrary[String], posNum[Int]) { (s: String, idx: Int) =>
      whenever(idx < s.length) {
        FastCharSequence(s).charAt(idx) shouldBe s.charAt(idx)
      }
    }
  }

  "subSequence(start)" should "behave like String's substring(start)" in {
    forAll(arbitrary[String], posNum[Int]) { (s: String, start: Int) =>
      whenever(start < s.length) {
        FastCharSequence(s).subSequence(start).toString shouldBe s.substring(start)
      }
    }
  }

  "subSequence(start, end)" should "behave like CharSequence's subSequence(start, end)" in {
    implicit val generatorDrivenConfig = PropertyCheckConfig(minSuccessful = 5)
    forAll(arbitrary[String], posNum[Int], posNum[Int]) { (s: String, start: Int, end: Int) =>
      whenever(start < s.length && end < s.length && start <= end) {
        FastCharSequence(s).subSequence(start, end).toString shouldBe s.subSequence(start, end).toString
      }
    }
  }

  "startsWith(chars)" should "behave like String's startsWith(string)" in {
    forAll { (s1: String, s2: String) =>
      FastCharSequence(s1).startWith(s2.toCharArray) shouldBe s1.startsWith(s2)
    }
  }
  "toString" should "behave like CharSequence's toString" in {
    forAll { (s: String) =>
      FastCharSequence(s).toString shouldBe s
    }
  }
}
