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

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class FastByteArrayOutputStreamSpec extends AnyFlatSpecLike with Matchers {

  "toByteArray" should "consume all bytes" in {
    val os = new FastByteArrayOutputStream(10)

    for {
      char <- '0' to '9'
      _ <- 0 until 10
    } os.write(char)

    new String(os.toByteArray, UTF_8) shouldBe (0 until 10)
      .foldLeft(new StringBuilder) { (acc, char) =>
        acc.append(char.toString * 10)
      }
      .toString
  }

  "writeTo" should "consume all bytes" in {
    val os = new FastByteArrayOutputStream(10)

    for {
      char <- '0' to '9'
      _ <- 0 until 10
    } os.write(char)

    val os2 = new ByteArrayOutputStream
    os.writeTo(os2)

    new String(os2.toByteArray, UTF_8) shouldBe (0 until 10)
      .foldLeft(new StringBuilder) { (acc, char) =>
        acc.append(char.toString * 10)
      }
      .toString
  }
}
