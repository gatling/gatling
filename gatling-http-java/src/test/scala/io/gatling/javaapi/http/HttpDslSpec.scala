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

package io.gatling.javaapi.http

import java.{ util => ju }

import io.gatling.commons.validation._
import io.gatling.core.{ EmptySession, Predef }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.client.body.multipart.StringPart

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class HttpDslSpec extends AnyFlatSpecLike with Matchers with EmptySession {

  private var previousConfiguration: GatlingConfiguration = _

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    previousConfiguration = Predef._configuration
    Predef._configuration = GatlingConfiguration.loadForTest()
  }

  override protected def afterAll(): Unit = {
    Predef._configuration = previousConfiguration
    super.afterAll()
  }

  "StringBodyPart" should "use the string returned by the function as the part's content" in {
    val bodyPart = HttpDsl.StringBodyPart((_: io.gatling.javaapi.core.Session) => "hello").asScala

    val part = bodyPart.partBuilder("name", None, None, None, None, None, new ju.ArrayList, None)(emptySession)

    part match {
      case Success(stringPart: StringPart) => stringPart.getContent shouldBe "hello"
      case other                           => fail(s"Expected a StringPart, got $other")
    }
  }
}
