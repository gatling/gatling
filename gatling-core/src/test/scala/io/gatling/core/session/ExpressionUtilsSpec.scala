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

package io.gatling.core.session

import java.util.Locale

import io.gatling.{ BaseSpec, ValidationValues }
import io.gatling.commons.validation._
import io.gatling.core.EmptySession
import io.gatling.core.session.el._

class ExpressionUtilsSpec extends BaseSpec with ValidationValues with EmptySession {

  "resolveOptionalExpression" should "return NoneSuccess if the expression was None" in {
    resolveOptionalExpression(None, emptySession) shouldBe NoneSuccess
  }

  it should "return the expression's result wrapped in a Success if the expression wasn't None" in {
    val session = emptySession.set("foo", "bar")
    val expr = Some("${foo}".el[String])
    resolveOptionalExpression(expr, session) shouldBe Success(Some("bar"))
  }

  "ExpressionWrapper" should "correctly map the underlying validation" in {
    val expr = "foo".el[String]
    expr(emptySession).succeeded shouldBe "foo"
    val newExpr = expr.map(_.toUpperCase(Locale.ROOT))
    newExpr(emptySession).succeeded shouldBe "FOO"
  }
}
