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

package io.gatling.core.check

import io.gatling.commons.validation._
import io.gatling.core.EmptySession
import io.gatling.core.session.Expression

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class PostCheckSpec extends AnyFlatSpecLike with Matchers with EmptySession {
  "postCheck" should "chain the postChecks on the resulting Session" in {
    val postChecks: List[Expression[io.gatling.core.session.Session]] = List(_.set("foo", 1).success, s => s.set("bar", s("foo").as[Int] + 1).success)

    val (session, failure) = Check.applyPostChecks(emptySession, None, postChecks)

    failure shouldBe None
    session("foo").as[Int] shouldBe 1
    session("bar").as[Int] shouldBe 2
  }

  it should "fail with the message of the failing postCheck" in {
    val (session, failure) = Check.applyPostChecks(emptySession, None, List(_ => "boom".failure, _.set("foo", 1).success))

    failure shouldBe Some(Failure("boom"))
    session("foo").as[Int] shouldBe 1
  }

  it should "turn a thrown Exception into a failure" in {
    val (_, failure) = Check.applyPostChecks(emptySession, None, List(_ => throw new IllegalStateException("boom")))

    failure.map(_.message).getOrElse("") should include("boom")
  }

  it should "keep the first failure" in {
    val (_, failure) = Check.applyPostChecks(emptySession, Some(Failure("check failure")), List(_ => "boom".failure))

    failure shouldBe Some(Failure("check failure"))
  }
}
