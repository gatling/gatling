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

import java.time.{ Instant, ZoneOffset, ZonedDateTime }
import java.time.format.DateTimeFormatter

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class LatestGatlingReleaseSpec extends AnyFlatSpecLike with Matchers {
  "parseMavenCentralResponse" should "successfully extra version" in {
    val response = """<?xml version="1.0" encoding="UTF-8"?>
                     |<metadata>
                     |  <groupId>io.gatling</groupId>
                     |  <artifactId>gatling-core</artifactId>
                     |  <versioning>
                     |    <latest>3.14.5</latest>
                     |    <release>3.14.5</release>
                     |    <versions>
                     |      <version>2.0.0-RC1</version>
                     |      <version>2.0.0-RC2</version>
                     |      <version>2.0.0-RC3</version>
                     |      <version>2.0.0-RC4</version>
                     |      <version>2.0.0-RC5</version>
                     |      <version>2.0.0-RC6</version>
                     |      <version>2.0.0</version>
                     |      <version>2.0.1</version>
                     |      <version>2.0.2</version>
                     |      <version>2.0.3</version>
                     |      <version>2.1.0</version>
                     |      <version>2.1.1</version>
                     |      <version>2.1.2</version>
                     |      <version>2.1.3</version>
                     |      <version>2.1.4</version>
                     |      <version>2.1.5</version>
                     |      <version>2.1.6</version>
                     |      <version>2.1.7</version>
                     |      <version>2.2.0-M1</version>
                     |      <version>2.2.0-M2</version>
                     |      <version>2.2.0-M3</version>
                     |      <version>2.2.0</version>
                     |      <version>2.2.1</version>
                     |      <version>2.2.2</version>
                     |      <version>2.2.3</version>
                     |      <version>2.2.4</version>
                     |      <version>2.2.5</version>
                     |      <version>2.3.0</version>
                     |      <version>2.3.1</version>
                     |      <version>3.0.0-RC1</version>
                     |      <version>3.0.0-RC2</version>
                     |      <version>3.0.0-RC3</version>
                     |      <version>3.0.0-RC4</version>
                     |      <version>3.0.0</version>
                     |      <version>3.0.1.1</version>
                     |      <version>3.0.1</version>
                     |      <version>3.0.2</version>
                     |      <version>3.0.3</version>
                     |      <version>3.1.0.1</version>
                     |      <version>3.1.0</version>
                     |      <version>3.1.1</version>
                     |      <version>3.1.2</version>
                     |      <version>3.1.3</version>
                     |      <version>3.2.0</version>
                     |      <version>3.2.1</version>
                     |      <version>3.3.0</version>
                     |      <version>3.3.1</version>
                     |      <version>3.4.0-M1</version>
                     |      <version>3.4.0</version>
                     |      <version>3.4.1</version>
                     |      <version>3.4.2</version>
                     |      <version>3.5.0</version>
                     |      <version>3.5.1</version>
                     |      <version>3.6.0</version>
                     |      <version>3.6.1</version>
                     |      <version>3.7.0-M1</version>
                     |      <version>3.7.0-M2</version>
                     |      <version>3.7.0-M3</version>
                     |      <version>3.7.0-M4</version>
                     |      <version>3.7.0</version>
                     |      <version>3.7.1</version>
                     |      <version>3.7.2</version>
                     |      <version>3.7.3</version>
                     |      <version>3.7.4</version>
                     |      <version>3.7.5</version>
                     |      <version>3.7.6</version>
                     |      <version>3.8.0</version>
                     |      <version>3.8.1</version>
                     |      <version>3.8.2</version>
                     |      <version>3.8.3</version>
                     |      <version>3.8.4</version>
                     |      <version>3.9.0</version>
                     |      <version>3.9.1</version>
                     |      <version>3.9.2</version>
                     |      <version>3.9.3</version>
                     |      <version>3.9.4</version>
                     |      <version>3.9.5</version>
                     |      <version>3.10.0</version>
                     |      <version>3.10.1</version>
                     |      <version>3.10.2</version>
                     |      <version>3.10.3</version>
                     |      <version>3.10.4</version>
                     |      <version>3.10.5</version>
                     |      <version>3.11.0</version>
                     |      <version>3.11.1</version>
                     |      <version>3.11.2</version>
                     |      <version>3.11.3</version>
                     |      <version>3.11.4</version>
                     |      <version>3.11.5</version>
                     |      <version>3.12.0</version>
                     |      <version>3.13.0</version>
                     |      <version>3.13.1</version>
                     |      <version>3.13.2</version>
                     |      <version>3.13.3</version>
                     |      <version>3.13.4</version>
                     |      <version>3.13.5</version>
                     |      <version>3.14.0</version>
                     |      <version>3.14.1</version>
                     |      <version>3.14.2</version>
                     |      <version>3.14.3</version>
                     |      <version>3.14.4</version>
                     |      <version>3.14.5</version>
                     |    </versions>
                     |    <lastUpdated>20250923110844</lastUpdated>
                     |  </versioning>
                     |</metadata>
                     |""".stripMargin
    LatestGatlingRelease.parseMavenCentralResponse(response) shouldBe GatlingVersion(
      "3.14.5",
      ZonedDateTime.parse("2025-09-23T11:08:44Z", DateTimeFormatter.ISO_ZONED_DATE_TIME)
    )
  }
}
