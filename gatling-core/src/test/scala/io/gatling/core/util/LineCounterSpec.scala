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

package io.gatling.core.util

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets._

import scala.util.Using

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class LineCounterSpec extends AnyFlatSpecLike with Matchers {
  private def testCount(text: String, expected: Int): Unit =
    Using.resource(new ByteArrayInputStream(text.getBytes(UTF_8))) { is =>
      new LineCounter(UTF_8, 5).countLines(is) shouldBe expected
    }

  "countLines" should "count lines when data is US_ASCII without empty lines" in {
    testCount(
      """foo
        |bar
        |baz""".stripMargin,
      3
    )
  }

  it should "ignore empty lines" in {
    testCount(
      """
        |foo
        |bar
        |
        |baz
        |""".stripMargin,
      3
    )
  }
}
