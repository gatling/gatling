/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.jsonpath

import io.gatling.jsonpath.AST._
import io.gatling.jsonpath.JsonPathParser._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.{ MatchResult, Matcher }
import org.scalatest.matchers.should.Matchers

class JsonPathParserSpec extends AnyFlatSpec with Matchers with ParsingMatchers {
  "Fast string replacement" should "work as expected" in {
    fastReplaceAll("foo", "f", "b") shouldBe "boo"
    fastReplaceAll("foobarqix", "bar", "B") shouldBe "fooBqix"
    fastReplaceAll("foo-foo-foo-bar-foo", "foo", "f") shouldBe "f-f-f-bar-f"
  }

  "Field parsing" should "work with standard names" in {
    def shouldParseField(name: String) = {
      val field = Field(name)
      parse(dotField(_), s".$name") should beParsedAs(field)
      parse(subscriptField(_), s"['$name']") should beParsedAs(field)
      parse(subscriptField(_), s"""["$name"]""") should beParsedAs(field)
    }

    shouldParseField("foo")
    shouldParseField("$foo")
    shouldParseField("Foo$1bar")
    shouldParseField("_1-2-3")
    shouldParseField("ñ1çölå$")
    shouldParseField("#token")
  }

  it should "work with the root object" in {
    parse(JsonPathParser.root(_), "$") should beParsedAs(RootNode)
  }

  it should "work when having multiple fields" in {
    parse(fieldAccessors(_), "['foo', 'bar', 'baz']") should beParsedAs(MultiField(List("foo", "bar", "baz")))
    parse(fieldAccessors(_), "['a', 'b c', 'd.e']") should beParsedAs(MultiField(List("a", "b c", "d.e")))
  }

  it should "support single quoted fields" in {
    parse(fieldAccessors(_), "['foo']") should beParsedAs(Field("foo"))
    parse(fieldAccessors(_), """['I\'m happy']""") should beParsedAs(Field("I'm happy"))
  }

  it should "support double quoted fields" in {
    parse(fieldAccessors(_), """["foo"]""") should beParsedAs(Field("foo"))
    parse(fieldAccessors(_), """["say \"hello\"!"]""") should beParsedAs(Field("""say "hello"!"""))
    parse(fieldAccessors(_), """["\"foo\""]""") should beParsedAs(Field("\"foo\""))
  }

  it should "support the 'any fields' syntax" in {
    parse(fieldAccessors(_), ".*") should beParsedAs(AnyField)
    parse(fieldAccessors(_), "['*']") should beParsedAs(AnyField)
    parse(fieldAccessors(_), """["*"]""") should beParsedAs(AnyField)
  }

  "Array parsing" should "work with random array accessors" in {
    parse(arrayAccessors(_), "[1]") should beParsedAs(ArrayRandomAccess(1 :: Nil))
    parse(arrayAccessors(_), "[42]") should beParsedAs(ArrayRandomAccess(42 :: Nil))
    parse(arrayAccessors(_), "[-1]") should beParsedAs(ArrayRandomAccess(-1 :: Nil))
    parse(arrayAccessors(_), "[-1,42 , -9]") should beParsedAs(ArrayRandomAccess(-1 :: 42 :: -9 :: Nil))
  }

  it should "should parse correctly the array slice operator" in {
    // 1 separator
    parse(arrayAccessors(_), "[:]") should beParsedAs(ArraySlice(None, None, 1))
    parse(arrayAccessors(_), "[2:]") should beParsedAs(ArraySlice(Some(2), None, 1))
    parse(arrayAccessors(_), "[:2]") should beParsedAs(ArraySlice(None, Some(2), 1))
    parse(arrayAccessors(_), "[2:4]") should beParsedAs(ArraySlice(Some(2), Some(4), 1))

    // 2 separators
    parse(arrayAccessors(_), "[::]") should beParsedAs(ArraySlice(None, None, 1))
    parse(arrayAccessors(_), "[::2]") should beParsedAs(ArraySlice(None, None, 2))
    parse(arrayAccessors(_), "[2::]") should beParsedAs(ArraySlice(Some(2), None, 1))
    parse(arrayAccessors(_), "[4::2]") should beParsedAs(ArraySlice(Some(4), None, 2))
    parse(arrayAccessors(_), "[:4:2]") should beParsedAs(ArraySlice(None, Some(4), 2))
    parse(arrayAccessors(_), "[0:8:2]") should beParsedAs(ArraySlice(Some(0), Some(8), 2))
  }

  it should "work with array access on the root object" in {
    parse("$[1]").get.value should be(RootNode :: ArrayRandomAccess(List(1)) :: Nil)
    parse("$[*]").get.value should be(RootNode :: ArraySlice.All :: Nil)
  }

  it should "work with array access on fields" in {
    parse(pathSequence(_), ".foo[1]").get.value should be(Field("foo") :: ArrayRandomAccess(List(1)) :: Nil)
    parse(pathSequence(_), ".ñ1çölå$[*]").get.value should be(Field("ñ1çölå$") :: ArraySlice.All :: Nil)
  }

  it should "work with array access on subscript fields" in {
    parse(pathSequence(_), "['foo'][1]").get.value should be(Field("foo") :: ArrayRandomAccess(List(1)) :: Nil)
    parse(pathSequence(_), "['ñ1çölå$'][*]").get.value should be(Field("ñ1çölå$") :: ArraySlice.All :: Nil)
  }

  "Dot fields" should "get parsed properly" in {
    parse(dotField(_), ".foo") should beParsedAs(Field("foo"))
    parse(dotField(_), ".ñ1çölå$") should beParsedAs(Field("ñ1çölå$"))
  }

  it should "work on the root element" in {
    parse("$.foo").get.value should be(RootNode :: Field("foo") :: Nil)
    parse("$['foo']").get.value should be(RootNode :: Field("foo") :: Nil)

    // TODO  : how to access childs w/ ['xxx'] notation
    parse("$..foo").get.value should be(RootNode :: RecursiveField("foo") :: Nil)
  }

  // cf : http://goessner.net/articles/JsonPath
  "Expressions from Goessner specs" should "be correctly parsed" in {
    def shouldParse(query: String, expected: Any) =
      parse(query).get.value should be(expected)

    shouldParse(
      "$.store.book[0].title",
      List(
        RootNode,
        Field("store"),
        Field("book"),
        ArrayRandomAccess(List(0)),
        Field("title")
      )
    )
    shouldParse(
      "$['store']['book'][0]['title']",
      List(
        RootNode,
        Field("store"),
        Field("book"),
        ArrayRandomAccess(List(0)),
        Field("title")
      )
    )
    shouldParse(
      "$.store.book[*].author",
      List(
        RootNode,
        Field("store"),
        Field("book"),
        ArraySlice.All,
        Field("author")
      )
    )
    shouldParse("$..author", List(RootNode, RecursiveField("author")))
    shouldParse("$.store.*", List(RootNode, Field("store"), AnyField))
    shouldParse("$.store..price", List(RootNode, Field("store"), RecursiveField("price")))
    shouldParse("$..*", List(RootNode, RecursiveAnyField))
    shouldParse("$.*", List(RootNode, AnyField))
    shouldParse("$..book[2]", List(RootNode, RecursiveField("book"), ArrayRandomAccess(List(2))))
    shouldParse("$.book[*]", List(RootNode, Field("book"), ArraySlice.All))
    shouldParse("$..book[*]", List(RootNode, RecursiveField("book"), ArraySlice.All))
    shouldParse(
      "$.store['store']..book['book'][0].title..title['title'].*..*.book[*]..book[*]",
      List(
        RootNode,
        Field("store"),
        Field("store"),
        RecursiveField("book"),
        Field("book"),
        ArrayRandomAccess(List(0)),
        Field("title"),
        RecursiveField("title"),
        Field("title"),
        AnyField,
        RecursiveAnyField,
        Field("book"),
        ArraySlice.All,
        RecursiveField("book"),
        ArraySlice.All
      )
    )
  }

  "Failures" should "be handled gracefully" in {
    def gracefulFailure(query: String): Unit =
      parse(query) match {
        case fail: fastparse.Parsed.Failure =>
          val msg = fail.msg
          info(s"""that's an expected failure for "$query": $msg""")
        case other =>
          fail(s"""a Failure was expected but instead, for "$query" got: $other""")
      }

    gracefulFailure("")
    gracefulFailure("foo")
    gracefulFailure("$f")
    gracefulFailure("$.[42]")
    gracefulFailure("$.[1:2,3]")
    gracefulFailure("$.[?(@.foo && 2)]")
  }

  "Filters" should "work with subqueries" in {
    parse(subscriptFilter(_), "[?(@..foo)]") should beParsedAs(
      HasFilter(SubQuery(List(CurrentNode, RecursiveField("foo"))))
    )
    parse(subscriptFilter(_), "[?(@.foo.baz)]") should beParsedAs(
      HasFilter(SubQuery(List(CurrentNode, Field("foo"), Field("baz"))))
    )
    parse(subscriptFilter(_), "[?(@['foo'])]") should beParsedAs(
      HasFilter(SubQuery(List(CurrentNode, Field("foo"))))
    )

    parse("$.things[?(@.foo.bar)]").get.value should be(
      RootNode
        :: Field("things")
        :: HasFilter(SubQuery(CurrentNode :: Field("foo") :: Field("bar") :: Nil))
        :: Nil
    )
  }

  it should "work with some predefined comparison operators" in {
    // Check all supported ordering operators
    parse(subscriptFilter(_), "[?(@ == 2)]") should beParsedAs(
      ComparisonFilter(EqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.long(2))
    )
    parse(subscriptFilter(_), "[?(@==2)]") should beParsedAs(
      ComparisonFilter(EqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.long(2))
    )
    parse(subscriptFilter(_), "[?(@ <= 2)]") should beParsedAs(
      ComparisonFilter(LessOrEqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.long(2))
    )
    parse(subscriptFilter(_), "[?(@<=2)]") should beParsedAs(
      ComparisonFilter(LessOrEqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.long(2))
    )
    parse(subscriptFilter(_), "[?(@ >= 2)]") should beParsedAs(
      ComparisonFilter(GreaterOrEqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.long(2))
    )
    parse(subscriptFilter(_), "[?(@>=2)]") should beParsedAs(
      ComparisonFilter(GreaterOrEqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.long(2))
    )
    parse(subscriptFilter(_), "[?(@ < 2)]") should beParsedAs(
      ComparisonFilter(LessOperator, SubQuery(List(CurrentNode)), FilterDirectValue.long(2))
    )
    parse(subscriptFilter(_), "[?(@<2)]") should beParsedAs(
      ComparisonFilter(LessOperator, SubQuery(List(CurrentNode)), FilterDirectValue.long(2))
    )
    parse(subscriptFilter(_), "[?(@ > 2)]") should beParsedAs(
      ComparisonFilter(GreaterOperator, SubQuery(List(CurrentNode)), FilterDirectValue.long(2))
    )
    parse(subscriptFilter(_), "[?(@>2)]") should beParsedAs(
      ComparisonFilter(GreaterOperator, SubQuery(List(CurrentNode)), FilterDirectValue.long(2))
    )
    parse(subscriptFilter(_), "[?(@ == true)]") should beParsedAs(
      ComparisonFilter(EqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.True)
    )
    parse(subscriptFilter(_), "[?(@==true)]") should beParsedAs(
      ComparisonFilter(EqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.True)
    )
    parse(subscriptFilter(_), "[?(@ != false)]") should beParsedAs(
      ComparisonFilter(NotEqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.False)
    )
    parse(subscriptFilter(_), "[?(@!=false)]") should beParsedAs(
      ComparisonFilter(NotEqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.False)
    )
    parse(subscriptFilter(_), "[?(@ == null)]") should beParsedAs(
      ComparisonFilter(EqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.Null)
    )
    parse(subscriptFilter(_), "[?(@==null)]") should beParsedAs(
      ComparisonFilter(EqOperator, SubQuery(List(CurrentNode)), FilterDirectValue.Null)
    )

    // Trickier Json path expressions
    parse(subscriptFilter(_), "[?(@.foo == 2)]") should beParsedAs(
      ComparisonFilter(EqOperator, SubQuery(List(CurrentNode, Field("foo"))), FilterDirectValue.long(2))
    )
    parse(subscriptFilter(_), "[?(true == @.foo)]") should beParsedAs(
      ComparisonFilter(EqOperator, FilterDirectValue.True, SubQuery(List(CurrentNode, Field("foo"))))
    )
    parse(subscriptFilter(_), "[?(2 == @['foo'])]") should beParsedAs(
      ComparisonFilter(EqOperator, FilterDirectValue.long(2), SubQuery(List(CurrentNode, Field("foo"))))
    )

    // Allow reference to the root object
    parse(subscriptFilter(_), "[?(@ == $['foo'])]") should beParsedAs(
      ComparisonFilter(EqOperator, SubQuery(List(CurrentNode)), SubQuery(List(RootNode, Field("foo"))))
    )

    parse("$['points'][?(@['y'] >= 3)].id").get.value should be(
      RootNode
        :: Field("points")
        :: ComparisonFilter(GreaterOrEqOperator, SubQuery(List(CurrentNode, Field("y"))), FilterDirectValue.long(3))
        :: Field("id") :: Nil
    )

    parse("$.points[?(@['id']=='i4')].x").get.value should be(
      RootNode
        :: Field("points")
        :: ComparisonFilter(EqOperator, SubQuery(List(CurrentNode, Field("id"))), FilterDirectValue.string("i4"))
        :: Field("x") :: Nil
    )

    parse("""$.points[?(@['id']=="i4")].x""").get.value should be(
      RootNode
        :: Field("points")
        :: ComparisonFilter(EqOperator, SubQuery(List(CurrentNode, Field("id"))), FilterDirectValue.string("i4"))
        :: Field("x") :: Nil
    )
  }

  it should "work with some predefined boolean operators" in {
    parse(subscriptFilter(_), "[?(@.foo && @.bar)]") should beParsedAs(
      BooleanFilter(
        AndOperator,
        HasFilter(SubQuery(List(CurrentNode, Field("foo")))),
        HasFilter(SubQuery(List(CurrentNode, Field("bar"))))
      )
    )

    parse(subscriptFilter(_), "[?(@.foo || @.bar)]") should beParsedAs(
      BooleanFilter(
        OrOperator,
        HasFilter(SubQuery(List(CurrentNode, Field("foo")))),
        HasFilter(SubQuery(List(CurrentNode, Field("bar"))))
      )
    )

    parse(subscriptFilter(_), "[?(@.foo || @.bar && @.quix)]") should beParsedAs(
      BooleanFilter(
        OrOperator,
        HasFilter(SubQuery(CurrentNode :: Field("foo") :: Nil)),
        BooleanFilter(
          AndOperator,
          HasFilter(SubQuery(CurrentNode :: Field("bar") :: Nil)),
          HasFilter(SubQuery(CurrentNode :: Field("quix") :: Nil))
        )
      )
    )

    parse(subscriptFilter(_), "[?(@.foo && @.bar || @.quix)]") should beParsedAs(
      BooleanFilter(
        OrOperator,
        BooleanFilter(
          AndOperator,
          HasFilter(SubQuery(CurrentNode :: Field("foo") :: Nil)),
          HasFilter(SubQuery(CurrentNode :: Field("bar") :: Nil))
        ),
        HasFilter(SubQuery(CurrentNode :: Field("quix") :: Nil))
      )
    )

    parse(subscriptFilter(_), "[?(@.foo || @.bar <= 2)]") should beParsedAs(
      BooleanFilter(
        OrOperator,
        HasFilter(SubQuery(List(CurrentNode, Field("foo")))),
        ComparisonFilter(LessOrEqOperator, SubQuery(List(CurrentNode, Field("bar"))), FilterDirectValue.long(2))
      )
    )
  }
}

trait ParsingMatchers {
  class SuccessBeMatcher[+T <: AstToken](expected: T) extends Matcher[fastparse.Parsed[AstToken]] {
    def apply(left: fastparse.Parsed[AstToken]): MatchResult =
      left match {
        case fastparse.Parsed.Success(res, _) =>
          MatchResult(
            expected == res,
            s"$res is not equal to expected value $expected",
            s"$res is equal to $expected but it shouldn't be"
          )
        case fail: fastparse.Parsed.Failure =>
          val msg = fail.msg
          MatchResult(
            matches = false,
            s"parsing issue, $msg",
            s"parsing issue, $msg"
          )
      }
  }

  def beParsedAs[T <: AstToken](expected: T): SuccessBeMatcher[T] = new SuccessBeMatcher(expected)
}
