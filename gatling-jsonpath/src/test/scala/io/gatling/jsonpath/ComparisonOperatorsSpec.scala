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

package io.gatling.jsonpath

import com.fasterxml.jackson.databind.node.{ BooleanNode, DoubleNode, FloatNode, IntNode, LongNode, TextNode }
import org.scalacheck.Arbitrary._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class ComparisonOperatorsSpec extends AnyFlatSpec with Matchers with ScalaCheckDrivenPropertyChecks {

  "comparison operators" should "return false if types aren't compatible" in {
    forAll(arbitrary[String], arbitrary[Int]) { (string, int) =>
      val lhn = new TextNode(string)
      val rhn = new IntNode(int)
      LessOperator(lhn, rhn) shouldBe false
      GreaterOperator(lhn, rhn) shouldBe false
      LessOrEqOperator(lhn, rhn) shouldBe false
      GreaterOrEqOperator(lhn, rhn) shouldBe false
    }

    forAll(arbitrary[Boolean], arbitrary[String]) { (bool, string) =>
      val lhn = BooleanNode.valueOf(bool)
      val rhn = new TextNode(string)
      LessOperator(lhn, rhn) shouldBe false
      GreaterOperator(lhn, rhn) shouldBe false
      LessOrEqOperator(lhn, rhn) shouldBe false
      GreaterOrEqOperator(lhn, rhn) shouldBe false
    }

    forAll(arbitrary[Int], arbitrary[String]) { (int, string) =>
      val lhn = new IntNode(int)
      val rhn = new TextNode(string)
      LessOperator(lhn, rhn) shouldBe false
      GreaterOperator(lhn, rhn) shouldBe false
      LessOrEqOperator(lhn, rhn) shouldBe false
      GreaterOrEqOperator(lhn, rhn) shouldBe false
    }
  }

  it should "properly compare Strings" in {
    forAll(arbitrary[String], arbitrary[String]) { (val1, val2) =>
      val lhn = new TextNode(val1)
      val rhn = new TextNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }
  }

  it should "properly compare Booleans" in {
    forAll(arbitrary[Boolean], arbitrary[Boolean]) { (val1, val2) =>
      val lhn = BooleanNode.valueOf(val1)
      val rhn = BooleanNode.valueOf(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }
  }

  it should "properly compare Int with other numeric types" in {
    forAll(arbitrary[Int], arbitrary[Int]) { (val1, val2) =>
      val lhn = new IntNode(val1)
      val rhn = new IntNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Int], arbitrary[Long]) { (val1, val2) =>
      val lhn = new IntNode(val1)
      val rhn = new LongNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Int], arbitrary[Double]) { (val1, val2) =>
      val lhn = new IntNode(val1)
      val rhn = new DoubleNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Int], arbitrary[Float]) { (val1, val2) =>
      val lhn = new IntNode(val1)
      val rhn = new FloatNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }
  }

  it should "properly compare Long with other numeric types" in {
    forAll(arbitrary[Long], arbitrary[Int]) { (val1, val2) =>
      val lhn = new LongNode(val1)
      val rhn = new IntNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Long], arbitrary[Long]) { (val1, val2) =>
      val lhn = new LongNode(val1)
      val rhn = new LongNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Long], arbitrary[Double]) { (val1, val2) =>
      val lhn = new LongNode(val1)
      val rhn = new DoubleNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Long], arbitrary[Float]) { (val1, val2) =>
      val lhn = new LongNode(val1)
      val rhn = new FloatNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }
  }

  it should "properly compare Double with other numeric types" in {
    forAll(arbitrary[Double], arbitrary[Int]) { (val1, val2) =>
      val lhn = new DoubleNode(val1)
      val rhn = new IntNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Double], arbitrary[Long]) { (val1, val2) =>
      val lhn = new DoubleNode(val1)
      val rhn = new LongNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Double], arbitrary[Double]) { (val1, val2) =>
      val lhn = new DoubleNode(val1)
      val rhn = new DoubleNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Double], arbitrary[Float]) { (val1, val2) =>
      val lhn = new DoubleNode(val1)
      val rhn = new FloatNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }
  }

  it should "properly compare Float with other numeric types" in {
    forAll(arbitrary[Float], arbitrary[Int]) { (val1, val2) =>
      val lhn = new FloatNode(val1)
      val rhn = new IntNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Float], arbitrary[Long]) { (val1, val2) =>
      val lhn = new FloatNode(val1)
      val rhn = new LongNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Float], arbitrary[Double]) { (val1, val2) =>
      val lhn = new FloatNode(val1)
      val rhn = new DoubleNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }

    forAll(arbitrary[Float], arbitrary[Float]) { (val1, val2) =>
      val lhn = new FloatNode(val1)
      val rhn = new FloatNode(val2)
      LessOperator(lhn, rhn) shouldBe (val1 < val2)
      GreaterOperator(lhn, rhn) shouldBe (val1 > val2)
      LessOrEqOperator(lhn, rhn) shouldBe (val1 <= val2)
      GreaterOrEqOperator(lhn, rhn) shouldBe (val1 >= val2)
    }
  }

  "AndOperator" should "&& the lhs and rhs" in {
    forAll(arbBool.arbitrary, arbBool.arbitrary) { (b1, b2) =>
      AndOperator(b1, b2) shouldBe (b1 && b2)
    }
  }

  "OrOperator" should "|| the lhs and rhs" in {
    forAll(arbBool.arbitrary, arbBool.arbitrary) { (b1, b2) =>
      OrOperator(b1, b2) shouldBe (b1 || b2)
    }
  }
}
