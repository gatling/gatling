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

package io.gatling.jms.check

import java.util.{ HashMap => JHashMap }
import javax.jms.Message

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.core.CoreDsl
import io.gatling.core.EmptySession
import io.gatling.core.check.CheckResult
import io.gatling.core.config.GatlingConfiguration
import io.gatling.jms.MockMessage

import org.scalatest.matchers.{ MatchResult, Matcher }
import org.scalatest.prop.{ TableDrivenPropertyChecks, TableFor2 }

class JmsJsonPathCheckSpec
    extends BaseSpec
    with ValidationValues
    with MockMessage
    with CoreDsl
    with JmsCheckSupport
    with TableDrivenPropertyChecks
    with EmptySession {
  override implicit def configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private def beIn[T](seq: Seq[T]) =
    new Matcher[T] {
      def apply(left: T): MatchResult =
        MatchResult(
          seq.contains(left),
          s"$left was not in $seq",
          s"$left was in $seq"
        )
    }

  private val testJson = s"""{ "store": {
                            |    "book": "In store"
                            |  },
                            |  "street": {
                            |    "book": "On the street"
                            |  },
                            |  "long_value": ${Long.MaxValue},
                            |  "null_value": null,
                            |  "not_null_value": "bar",
                            |  "documents":[]
                            |}""".stripMargin

  val jsons: TableFor2[Message, String] = Table(
    ("json", "messageType"),
    (textMessage(testJson), "TextMessage"),
    (bytesMessage(testJson.getBytes(configuration.core.charset)), "BytesMessage")
  )

  forAll(jsons) { (response, messageType) =>
    s"jsonPath.find for $messageType" should "support long values" in {
      jsonPath("$.long_value").ofType[Long].find.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(
        Some(Long.MaxValue),
        None
      )
    }

    s"jsonPath.find.exists for $messageType" should "find single result into JSON serialized form" in {
      jsonPath("$.street").find.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(
        Some("""{"book":"On the street"}"""),
        None
      )
    }

    it should "find single result into Map object form" in {
      jsonPath("$.street").ofType[Map[String, Any]].find.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(
        Some(Map("book" -> "On the street")),
        None
      )
    }

    it should "find a null attribute value when expected type is String" in {
      jsonPath("$.null_value").ofType[String].find.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(null), None)
    }

    it should "find a null attribute value when expected type is Any" in {
      jsonPath("$.null_value").ofType[Any].find.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(null), None)
    }

    it should "find a null attribute value when expected type is Int" in {
      jsonPath("$.null_value").ofType[Int].find.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(
        Some(null.asInstanceOf[Int]),
        None
      )
    }

    it should "find a null attribute value when expected type is Seq" in {
      jsonPath("$.null_value").ofType[Seq[Any]].find.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(
        Some(null),
        None
      )
    }

    it should "find a null attribute value when expected type is Map" in {
      jsonPath("$.null_value").ofType[Map[String, Any]].find.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(
        Some(null),
        None
      )
    }

    it should "succeed when expecting a null value and getting a null one" in {
      jsonPath("$.null_value").ofType[Any].find.isNull.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(Some(null), None)
    }

    it should "fail when expecting a null value and getting a non-null one" in {
      jsonPath("$.not_null_value")
        .ofType[Any]
        .find
        .isNull
        .check(response, emptySession, new JHashMap[Any, Any])
        .failed shouldBe "jsonPath($.not_null_value).find.isNull, but actually found bar"
    }

    it should "succeed when expecting a non-null value and getting a non-null one" in {
      jsonPath("$.not_null_value").ofType[Any].find.notNull.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(
        Some("bar"),
        None
      )
    }

    it should "fail when expecting a non-null value and getting a null one" in {
      jsonPath("$.null_value")
        .ofType[Any]
        .find
        .notNull
        .check(response, emptySession, new JHashMap[Any, Any])
        .failed shouldBe "jsonPath($.null_value).find.notNull, but actually found null"
    }

    it should "not fail on empty array" in {
      jsonPath("$.documents").ofType[Seq[_]].find.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(
        Some(Vector.empty),
        None
      )
    }

    s"jsonPath.findAll.exists for $messageType" should "fetch all matches" in {

      jsonPath("$..book").findAll.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(
        Some(Seq("In store", "On the street")),
        None
      )
    }

    it should "find all by wildcard" in {
      jsonPath("$.*.book").findAll.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(
        Some(Vector("In store", "On the street")),
        None
      )
    }

    s"jsonPath.findRandom.exists for $messageType" should "fetch a single random match" in {
      jsonPath("$..book").findRandom.exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded.extractedValue.get.asInstanceOf[String] should beIn(
        Seq("In store", "On the street")
      )
    }

    it should "fetch at max num results" in {
      jsonPath("$..book")
        .findRandom(1)
        .exists
        .check(response, emptySession, new JHashMap[Any, Any])
        .succeeded
        .extractedValue
        .get
        .asInstanceOf[Seq[String]] should beIn(Seq(Seq("In store"), Seq("On the street")))
    }

    it should "fetch all the matches when expected number is greater" in {
      jsonPath("$..book").findRandom(3).exists.check(response, emptySession, new JHashMap[Any, Any]).succeeded shouldBe CheckResult(
        Some(Seq("In store", "On the street")),
        None
      )
    }

    it should "fail when failIfLess is enabled and expected number is greater" in {
      jsonPath("$..book").findRandom(3, failIfLess = true).exists.check(response, emptySession, new JHashMap[Any, Any]).failed
    }
  }
}
