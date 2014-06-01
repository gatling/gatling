/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.fetch

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.{ Before, Specification }

@RunWith(classOf[JUnitRunner])
class ConditionalCommentSpec extends Specification {

  trait Browser extends Before {
    def ie(version: Double): ConditionalComment = {
      new ConditionalComment(ConditionalComment.IE, version)
    }

    def before() {}
  }

  "conditional comment" should {
    "return false if browser is not IE" in {
      val cc = new ConditionalComment("Firefox", 5)
      cc.evaluate("if true") should beFalse
    }

    "return false if IE version is greater or equals then 10" in new Browser {
      ie(10).evaluate("if true") should beFalse
    }

    "evaluate true" in new Browser {
      ie(9).evaluate("if true") should beTrue
    }

    "evaluate false" in new Browser {
      ie(9).evaluate("if false") should beFalse
    }

    "evaluate same version" in new Browser {
      ie(9).evaluate("if IE 9") should beTrue
    }

    "evaluate different version" in new Browser {
      ie(9).evaluate("if IE 8") should beFalse
    }

    "evaluate not operator" in new Browser {
      ie(9).evaluate("if !IE 9") should beFalse
    }

    "evaluate not operator" in new Browser {
      ie(9).evaluate("if !IE 9") should beFalse
    }

    "evaluate gt operator" in new Browser {
      ie(9).evaluate("if gt IE 5") should beTrue
    }

    "evaluate gt operator" in new Browser {
      ie(5).evaluate("if gt IE 5") should beFalse
    }

    "evaluate lt operator" in new Browser {
      ie(5).evaluate("if lt IE 9") should beTrue
    }

    "evaluate lt operator" in new Browser {
      ie(5).evaluate("if lt IE 5") should beFalse
    }

    "evaluate gte operator" in new Browser {
      ie(9).evaluate("if gte IE 5") should beTrue
    }

    "evaluate gte operator" in new Browser {
      ie(5).evaluate("if gte IE 5") should beTrue
    }

    "evaluate gte operator" in new Browser {
      ie(4).evaluate("if gte IE 5") should beFalse
    }

    "evaluate lte operator" in new Browser {
      ie(5).evaluate("if lte IE 9") should beTrue
    }

    "evaluate lte operator" in new Browser {
      ie(5).evaluate("if lte IE 5") should beTrue
    }

    "evaluate ! parentheses " in new Browser {
      ie(5).evaluate("if !(IE 5)") should beFalse
    }

    "evaluate ! gt parentheses" in new Browser {
      ie(9).evaluate("if !(gt IE 5)") should beFalse
    }

    "& truth table" in new Browser {
      ie(9).evaluate("if true&true") should beTrue
      ie(9).evaluate("if true&false") should beFalse
      ie(9).evaluate("if false&true") should beFalse
      ie(9).evaluate("if false&false") should beFalse
    }

    "| truth table" in new Browser {
      ie(9).evaluate("if true|true") should beTrue
      ie(9).evaluate("if true|false") should beTrue
      ie(9).evaluate("if false|true") should beTrue
      ie(9).evaluate("if false|false") should beFalse
    }

    "evaluate & parentheses" in new Browser {
      ie(9).evaluate("if (lt IE 10)&(gt IE 5)") should beTrue
    }

    "evaluate & parentheses" in new Browser {
      ie(9).evaluate("if (gt IE 10)&(lt IE 5)") should beFalse
    }

    "evaluate & parentheses" in new Browser {
      ie(9).evaluate("if (lte IE 9)|(gt IE 10)") should beTrue
    }

    "operators precedence" in new Browser {
      ie(9).evaluate("if false | true & false | true") should beTrue
    }

    "complex & | expression" in new Browser {
      ie(9).evaluate("if ((gt IE 10)|(gt IE 8)) & ((lt IE 5)|(lt IE 10))") should beTrue
    }

    "double version equality" in new Browser {
      ie(5.5).evaluate("if IE 5.5") should beTrue
      ie(5.5).evaluate("if IE 5") should beTrue
      ie(5.5).evaluate("if IE 6") should beFalse
    }
  }
}
