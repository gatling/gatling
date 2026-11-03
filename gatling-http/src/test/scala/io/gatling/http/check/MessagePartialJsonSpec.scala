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

package io.gatling.http.check

import scala.concurrent.duration._

import io.gatling.commons.validation.Failure
import io.gatling.core.{ CoreDsl, EmptySession }
import io.gatling.core.check.{ Check, PreparedCheck, StringJsonPreparer, StringPartialJsonPreparer }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.http.HttpDsl
import io.gatling.http.action.sse.SseMessageCheckSequenceBuilder
import io.gatling.http.action.ws.WsFrameCheckSequenceBuilder
import io.gatling.http.check.sse.SseMessageCheck
import io.gatling.http.check.ws.{ WsCheck, WsFrameCheck }

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class MessagePartialJsonSpec extends AnyFlatSpecLike with Matchers with CoreDsl with HttpDsl with EmptySession {
  override implicit val configuration: GatlingConfiguration = GatlingConfiguration.loadForTest()

  private val Message = """{"id":"1","type":"next","payload":{"value":"v","big":[1,2,3]}}"""

  private def preparers(checks: List[Check[String]]): List[Any] =
    checks.collect { case prepared: PreparedCheck[_] => prepared.preparer: Any }

  private def wsPreparers(frameCheck: WsFrameCheck.Text): List[Any] =
    preparers((frameCheck.matchConditions ::: frameCheck.checks).map(_.wrapped))

  private def ssePreparers(messageCheck: SseMessageCheck): List[Any] =
    preparers((messageCheck.matchConditions ::: messageCheck.checks).map(_.wrapped))

  /** Applies the match conditions then the checks on the same PreparedCache, like the WebSocket and SSE state machines do. */
  private def applied(matchConditions: List[Check[String]], checks: List[Check[String]], message: String): (Boolean, Option[Failure], Int) = {
    val preparedCache = Check.newPreparedCache
    val matches = matchConditions.forall(_.check(message, emptySession, preparedCache).toOption.isDefined)
    val (_, failure) = Check.applyChecks(message, emptySession, checks, preparedCache)
    (matches, failure, preparedCache.size)
  }

  "WebSocket JSON checks" should "share a partial Preparer with the match conditions when they all read a subset" in {
    val frameCheck = ws
      .checkTextMessage("check")
      .matching(jsonPath("$.id").is("1"))
      .check(jmesPath("payload.value").is("v"), jsonPath("$.type").is("next"))
      .restrictedToJsonSubset

    val preparers = wsPreparers(frameCheck)
    preparers should have size 3
    preparers.distinct should have size 1
    preparers.head shouldBe a[StringPartialJsonPreparer]

    applied(frameCheck.matchConditions, frameCheck.checks, Message) shouldBe ((true, None, 1))
    applied(frameCheck.matchConditions, frameCheck.checks, Message.replace("\"1\"", "\"2\""))._1 shouldBe false
  }

  it should "parse the whole message when a check may read anything" in {
    val frameCheck = ws
      .checkTextMessage("check")
      .matching(jsonPath("$.id").is("1"))
      .check(jsonPath("$..value").is("v"))
      .restrictedToJsonSubset

    wsPreparers(frameCheck) shouldBe List(StringJsonPreparer(defaultJsonParsers), StringJsonPreparer(defaultJsonParsers))
    applied(frameCheck.matchConditions, frameCheck.checks, Message) shouldBe ((true, None, 1))
  }

  it should "parse the whole message again when restricted checks get one that may read anything" in {
    val restricted = ws.checkTextMessage("check").check(jsonPath("$.id").is("1")).restrictedToJsonSubset
    wsPreparers(restricted).head shouldBe a[StringPartialJsonPreparer]

    val frameCheck = restricted.check(jsonPath("$..value").is("v")).restrictedToJsonSubset
    wsPreparers(frameCheck) shouldBe List(StringJsonPreparer(defaultJsonParsers), StringJsonPreparer(defaultJsonParsers))
  }

  "SSE JSON checks" should "share a partial Preparer with the match conditions when they all read a subset" in {
    val messageCheck = sse
      .checkMessage("check")
      .matching(jsonPath("$.id").is("1"))
      .check(jmesPath("payload.value").is("v"))
      .restrictedToJsonSubset

    val preparers = ssePreparers(messageCheck)
    preparers should have size 2
    preparers.distinct should have size 1
    preparers.head shouldBe a[StringPartialJsonPreparer]

    applied(messageCheck.matchConditions, messageCheck.checks, Message) shouldBe ((true, None, 1))
  }

  "Check sequences" should "only parse the subset of the messages their checks read once resolved" in {
    val frameCheck = ws.checkTextMessage("check").matching(jsonPath("$.id").is("1")).check(jsonPath("$.type").is("next"))
    val wsResolved = WsFrameCheckSequenceBuilder
      .resolve(List(WsFrameCheckSequenceBuilder(1.second.expressionSuccess, List(frameCheck))), emptySession)
      .toOption
      .flatMap(_.headOption)
      .getOrElse(fail("expected a resolved sequence"))
    wsResolved.checks.map(wsPreparers).flatten.distinct.map(_.getClass) shouldBe List(classOf[StringPartialJsonPreparer])

    val messageCheck = sse.checkMessage("check").matching(jsonPath("$.id").is("1")).check(jsonPath("$.type").is("next"))
    val sseResolved = SseMessageCheckSequenceBuilder
      .resolve(List(SseMessageCheckSequenceBuilder(1.second.expressionSuccess, List(messageCheck))), emptySession)
      .toOption
      .flatMap(_.headOption)
      .getOrElse(fail("expected a resolved sequence"))
    sseResolved.checks.map(ssePreparers).flatten.distinct.map(_.getClass) shouldBe List(classOf[StringPartialJsonPreparer])
  }

  "Non JSON checks" should "be left untouched" in {
    val regexCheck: WsCheck.Text = regex("next")
    val frameCheck = ws.checkTextMessage("check").check(regexCheck, jsonPath("$.id").is("1")).restrictedToJsonSubset

    frameCheck.checks.head shouldBe theSameInstanceAs(regexCheck)
    applied(Nil, frameCheck.checks, Message) shouldBe ((true, None, 2))
  }
}
