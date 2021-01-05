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

package io.gatling.core.check.css

import io.gatling.BaseSpec

import jodd.lagarto.dom.Node

class FormExtractorSpec extends BaseSpec {

  private val cssSelectors = new CssSelectors(Long.MaxValue)

  private def formInputs(html: String): Map[String, Any] = {
    val nodeSelector = cssSelectors.parse(html.toCharArray)
    val node = cssSelectors.extractAll[Node](nodeSelector, ("form", None)).head
    FormExtractor.extractFormInputs(node)
  }

  "FormExtractor on empty form" should "extract nothing" in {
    val inputs = formInputs(
      """<form>
        |<span>foo</span>
        |</form>
      """.stripMargin
    )

    inputs shouldBe empty
  }

  "FormExtractor with standard input" should "extract when input is form child" in {
    val inputs = formInputs(
      """<form>
        | <span>foo</span>
        | <input type="text" name="foo" value="bar">
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some("bar")
  }

  it should "extract when input is not a direct form child" in {
    val inputs = formInputs(
      """<form>
        | <input type="text" name="foo" value="bar">
        | <div>
        |   <input type="text" name="baz" value="qix">
        | </div>
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 2
    inputs.get("foo") shouldBe Some("bar")
    inputs.get("baz") shouldBe Some("qix")
  }

  it should "extract multivalued when name occurs multiple times" in {
    val inputs = formInputs(
      """<form>
        | <input type="text" name="foo" value="bar">
        |   <input type="text" name="foo" value="baz">
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some(Seq("bar", "baz"))
  }

  "FormExtractor with select" should "extract first option when no option is selected" in {
    val inputs = formInputs(
      """<form>
        | <select name="foo">
        |   <option value="opt1">opt1</option>
        |   <option value="opt2">opt2</option>
        | </select>
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some("opt1")
  }

  it should "extract monovalue when one option is selected" in {
    val inputs = formInputs(
      """<form>
        | <select name="foo">
        |   <option value="opt1" selected>opt1</option>
        |   <option value="opt2">opt2</option>
        | </select>
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some("opt1")
  }

  it should "extract multivalued when multiple attribute is set and single option is selected" in {
    val inputs = formInputs(
      """<form>
        | <select name="foo" multiple>
        |   <option value="opt1" selected>opt1</option>
        |   <option value="opt2">opt2</option>
        | </select>
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some(Seq("opt1"))
  }

  it should "extract multivalued when multiple attribute is set and multiple options are selected" in {
    val inputs = formInputs(
      """<form>
        | <select name="foo" multiple>
        |   <option value="opt1" selected>opt1</option>
        |   <option value="opt2" selected>opt2</option>
        | </select>
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some(Seq("opt1", "opt2"))
  }

  "FormExtractor with radio" should "extract nothing when none is checked" in {
    val inputs = formInputs(
      """<form>
        | <input type="radio" name"foo" value="bar">
        | <input type="radio" name"foo" value="baz">
        | <input type="radio" name"foo" value="qix">
        |</form>
      """.stripMargin
    )

    inputs shouldBe empty
  }

  it should "extract monovalued when one is checked" in {
    val inputs = formInputs(
      """<form>
        | <input type="radio" name="foo" value="bar" checked>
        | <input type="radio" name="foo" value="baz">
        | <input type="radio" name="foo" value="qix">
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some("bar")
  }

  "FormExtractor with checkbox" should "extract monovalued when form contains only one occurrence of the name" in {
    val inputs = formInputs(
      """<form>
        | <input type="checkbox" name="foo" value="bar" checked>
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some("bar")
  }

  it should "extract multivalued when form contains multiple occurrences of the name, even if only one is checked" in {
    val inputs = formInputs(
      """<form>
        | <input type="checkbox" name="foo" value="bar" checked>
        | <input type="checkbox" name="foo" value="baz">
        | <input type="checkbox" name="foo" value="qix">
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some(Seq("bar"))
  }

  it should "extract multivalued when form contains multiple occurrences of the name and several are checked" in {
    val inputs = formInputs(
      """<form>
        | <input type="checkbox" name="foo" value="bar" checked>
        | <input type="checkbox" name="foo" value="baz" checked>
        | <input type="checkbox" name="foo" value="qix">
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some(Seq("bar", "baz"))
  }

  "FormExtractor with non actionable inputs" should "ignore them" in {
    val inputs = formInputs(
      """<form>
        | <input type="submit" name="foo" value="foo">
        | <input type="button" name="bar" value="bar">
        | <input type="reset" name="baz" value="baz">
        | <input type="file" name="qix">
        |</form>
      """.stripMargin
    )

    inputs shouldBe empty
  }

  "FormExtractor with disabled inputs" should "ignore them" in {
    val inputs = formInputs(
      """<form>
        | <input type="text" name="foo" value="foo" disabled>
        |</form>
      """.stripMargin
    )

    inputs shouldBe empty
  }

  "FormExtractor with textarea" should "extract content" in {
    val inputs = formInputs(
      """<form>
        | <textarea name="foo">foo</textarea>
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some("foo")
  }

  it should "extract empty String when content is empty" in {
    val inputs = formInputs(
      """<form>
        | <textarea name="foo"></textarea>
        |</form>
      """.stripMargin
    )

    inputs.size shouldBe 1
    inputs.get("foo") shouldBe Some("")
  }
}
