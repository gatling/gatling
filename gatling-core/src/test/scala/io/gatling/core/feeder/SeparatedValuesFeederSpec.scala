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

package io.gatling.core.feeder

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import java.nio.channels.{ Channels, ReadableByteChannel }
import java.nio.charset.StandardCharsets.UTF_8

import scala.util.Using

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.feeder.SeparatedValuesParser._
import io.gatling.core.feeder.Utf8BomSkipReadableByteChannel._

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class SeparatedValuesFeederSpec extends AnyFlatSpecLike with Matchers with FeederSupport {
  private implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  "csv" should "not handle file without quote char" in {
    val data = csv("sample1.tsv").readRecords
    data should not be Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with quote char" in {
    val data = csv("sample2.csv").readRecords
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "be compliant with the RFC4180 by default and use \" as escape char" in {
    val data = csv("sample4.csv").readRecords
    data shouldBe Array(Map("id" -> "id", "payload" -> """{"key1": "value1", "key2": "value3"}"""))
  }

  it should "handle file without a header line when headers are provided" in {
    val data = csv("sample-no-headers.csv").headers("foo", "bar").readRecords
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"), Map("foo" -> "bonjour", "bar" -> "monde"))
  }

  it should "not drop the first line when headers are provided" in {
    csv("sample-no-headers.csv").headers("foo", "bar").recordsCount shouldBe 2
    csv("sample2.csv").recordsCount shouldBe 1
  }

  it should "reject blank headers" in {
    an[IllegalArgumentException] should be thrownBy csv("sample-no-headers.csv").headers("foo", "")
  }

  it should "reject duplicated headers" in {
    an[IllegalArgumentException] should be thrownBy csv("sample-no-headers.csv").headers("foo", "foo")
  }

  "tsv" should "handle file without quote char" in {
    val data = tsv("sample1.tsv").readRecords
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with quote char" in {
    val data = tsv("sample2.tsv").readRecords
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  "ssv" should "not handle file without quote char" in {
    val data = ssv("sample1.ssv").readRecords
    data should not be Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "handle file with quote char" in {
    val data = ssv("sample2.ssv").readRecords
    data shouldBe Array(Map("foo" -> "hello", "bar" -> "world"))
  }

  private def newChannel(bytes: Array[Byte]): ReadableByteChannel =
    Channels.newChannel(new ByteArrayInputStream(bytes))

  "SeparatedValuesParser.feederFactory" should "throw an exception when provided with bad resource" in {
    an[Exception] should be thrownBy
      feederFactory(CommaSeparator, quoteChar = '\'', configuration.core.charset)(newChannel(Array.emptyByteArray))
  }

  it should "skip UTF-8 BOM" in {
    val bytes =
      Using.resource(new ByteArrayOutputStream) { os =>
        os.write(Array(Utf8BomByte1, Utf8BomByte2, Utf8BomByte3))
        os.write("foo,bar\n".getBytes(UTF_8))
        os.write("hello,world\n".getBytes(UTF_8))
        os.toByteArray
      }
    feederFactory(CommaSeparator, quoteChar = '\'', UTF_8)(newChannel(bytes)).toVector shouldBe Vector(Map("foo" -> "hello", "bar" -> "world"))
  }

  it should "skip empty lines" in {
    val bytes =
      """header
        |line1
        |
        |line2
        |
        |""".stripMargin.getBytes(UTF_8)

    feederFactory(CommaSeparator, quoteChar = '\'', UTF_8)(newChannel(bytes)).toVector shouldBe Vector(
      Map("header" -> "line1"),
      Map("header" -> "line2")
    )
  }

  "SeparatedValuesParser.headerlessFeederFactory" should "throw an exception when provided with bad resource" in {
    an[Exception] should be thrownBy
      headerlessFeederFactory(CommaSeparator, quoteChar = '\'', configuration.core.charset, Seq("foo", "bar"))(newChannel(Array.emptyByteArray))
  }

  it should "skip UTF-8 BOM" in {
    val bytes =
      Using.resource(new ByteArrayOutputStream) { os =>
        os.write(Array(Utf8BomByte1, Utf8BomByte2, Utf8BomByte3))
        os.write("hello,world\n".getBytes(UTF_8))
        os.toByteArray
      }
    headerlessFeederFactory(CommaSeparator, quoteChar = '\'', UTF_8, Seq("foo", "bar"))(newChannel(bytes)).toVector shouldBe Vector(
      Map("foo" -> "hello", "bar" -> "world")
    )
  }

  it should "skip empty lines" in {
    val bytes =
      """line1
        |
        |line2
        |
        |""".stripMargin.getBytes(UTF_8)

    headerlessFeederFactory(CommaSeparator, quoteChar = '\'', UTF_8, Seq("header"))(newChannel(bytes)).toVector shouldBe Vector(
      Map("header" -> "line1"),
      Map("header" -> "line2")
    )
  }
}
