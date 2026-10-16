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
import io.gatling.core.session.Session

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class VerifySpec extends AnyFlatSpecLike with Matchers with EmptySession {
  private def verify(f: (Option[Int], Session) => Option[String]): Validator[Int] = {
    val validate = CheckBuilder.Validate.Default[Unit, Unit, Int](null, logActualValueInError = true)
    validate.verify("positive", f) match {
      case f: CheckBuilder.Final.Default[Unit, Unit, Int] @unchecked =>
        val v = f.validator(emptySession).toOption.get
        v.name shouldBe "positive"
        v
      case other => fail(s"Unexpected Final $other")
    }
  }

  "verify" should "succeed with the actual value when the function returns None" in {
    verify((_, _) => None)(Some(1), logActualValueInError = true) shouldBe Success(Some(1))
  }

  it should "fail with the returned error message" in {
    verify((actual, _) => if (actual.exists(_ > 0)) None else Some("not positive"))(Some(-1), logActualValueInError = true) shouldBe Failure("not positive")
  }

  it should "pass None when nothing was extracted" in {
    verify((actual, _) => if (actual.isEmpty) Some("nothing") else None)(None, logActualValueInError = true) shouldBe Failure("nothing")
  }

  it should "turn a thrown Exception into a failure" in {
    verify((_, _) => throw new IllegalStateException("boom"))(Some(1), logActualValueInError = true) match {
      case Failure(message) => message should include("boom")
      case other            => fail(s"Unexpected $other")
    }
  }
}
