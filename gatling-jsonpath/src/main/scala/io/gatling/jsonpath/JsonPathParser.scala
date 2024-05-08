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
import io.gatling.shared.util.StringBuilderPool

import fastparse._
import fastparse.MultiLineWhitespace._

/**
 * Originally contributed by Nicolas Rémond.
 */
private[jsonpath] object JsonPathParser {
  private val stringBuilderPool = new StringBuilderPool

  private[jsonpath] def fastReplaceAll(
      text: String,
      replaced: String,
      replacement: String
  ): String = {
    var end = text.indexOf(replaced)
    if (end == -1) {
      text
    } else {
      var start = 0
      val replacedLength = replaced.length
      val buf = stringBuilderPool.get()
      while (end != -1) {
        buf.append(text, start, end).append(replacement)
        start = end + replacedLength
        end = text.indexOf(replaced, start)
      }
      buf.append(text, start, text.length).toString
    }
  }

  private val specialCharacters =
    Set('*', '.', '[', ']', '(', ')', '=', '!', '<', '>')

  /// general purpose parsers ///////////////////////////////////////////////

  private def number[$: P]: P[Int] =
    P(("-".? ~~ CharsWhileIn("0-9", 1)).!).map(_.toInt)
  private def field[$: P]: P[String] = P(
    CharsWhile(char => !char.isWhitespace && !specialCharacters(char), 1).!
  )
  private def quoted[$: P](quote: Char, minChars: Int): P[String] =
    P(
      s"$quote" ~~ (CharsWhile(c => c != quote && c != '\\') | ("\\" ~~ AnyChar)).repX(minChars).! ~~ s"$quote"
    ).map(fastReplaceAll(_, s"\\$quote", s"$quote"))

  private def singleQuotedField[$: P]: P[String] = quoted('\'', 1)
  private def doubleQuotedField[$: P]: P[String] = quoted('\"', 1)
  private def singleQuotedValue[$: P]: P[String] = quoted('\'', 0)
  private def doubleQuotedValue[$: P]: P[String] = quoted('\"', 0)
  private def quotedField[$: P]: P[String] = P(
    singleQuotedField | doubleQuotedField
  )
  private def quotedValue[$: P]: P[String] = P(
    singleQuotedValue | doubleQuotedValue
  )

  private def floatString[$: P]: P[String] = P(
    ("-".? ~~ CharsWhileIn("0-9", 1) ~~ ("." ~~ CharsWhileIn("0-9").?).?).!
  )

  /// array parsers /////////////////////////////////////////////////////////

  private def arraySliceStep[$: P]: P[Option[Int]] = P(":" ~ number.?)

  private def arraySlice[$: P]: P[ArraySlice] =
    P(arraySliceStep ~ arraySliceStep.?).map { case (end, step) =>
      ArraySlice(None, end, step.flatten.getOrElse(1))
    }

  private def arraySlicePartial[$: P]: P[ArrayAccessor] =
    P(number ~ arraySlice).map { case (i, as) => as.copy(start = Some(i)) }

  private def arrayRandomAccessPartial[$: P]: P[ArrayAccessor] =
    P(number.rep(1, ",")).map(indices => ArrayRandomAccess(indices.toList))

  private def arrayPartial[$: P]: P[ArrayAccessor] =
    P(arraySlicePartial | arrayRandomAccessPartial)

  private def arrayAll[$: P]: P[ArraySlice] =
    P("*").map(_ => ArraySlice.All)

  private[jsonpath] def arrayAccessors[$: P]: P[ArrayAccessor] =
    P("[" ~ (arrayAll | arrayPartial | arraySlice) ~ "]")

  /// filters parsers ///////////////////////////////////////////////////////

  private def numberValue[$: P]: P[FilterDirectValue] = floatString.map { s =>
    if (s.indexOf('.') != -1) FilterDirectValue.double(s.toDouble)
    else FilterDirectValue.long(s.toLong)
  }

  private def booleanValue[$: P]: P[FilterDirectValue] =
    P(
      P("true").map(_ => FilterDirectValue.True) |
        P("false").map(_ => FilterDirectValue.False)
    )

  private def nullValue[$: P]: P[FilterValue] =
    P("null").map(_ => FilterDirectValue.Null)

  private def stringValue[$: P]: P[FilterDirectValue] = quotedValue.map {
    FilterDirectValue.string
  }
  private def value[$: P]: P[FilterValue] = P(
    booleanValue | numberValue | nullValue | stringValue
  )

  private def comparisonOperator[$: P]: P[ComparisonOperator] =
    P(
      P("==").map(_ => EqOperator) |
        P("!=").map(_ => NotEqOperator) |
        P("<=").map(_ => LessOrEqOperator) |
        P("<").map(_ => LessOperator) |
        P(">=").map(_ => GreaterOrEqOperator) |
        P(">").map(_ => GreaterOperator)
    )

  private def current[$: P]: P[PathToken] = P("@").map(_ => CurrentNode)

  private def subQuery[$: P]: P[SubQuery] =
    P((current | root) ~ pathSequence).map { case (c, ps) => SubQuery(c :: ps) }

  private def expression1[$: P]: P[FilterToken] =
    P(subQuery ~ (comparisonOperator ~ (subQuery | value)).?).map {
      case (subq1, None)          => HasFilter(subq1)
      case (lhs, Some((op, rhs))) => ComparisonFilter(op, lhs, rhs)
    }

  private def expression2[$: P]: P[FilterToken] =
    P(value ~ comparisonOperator ~ subQuery).map { case (lhs, op, rhs) =>
      ComparisonFilter(op, lhs, rhs)
    }

  private def expression[$: P]: P[FilterToken] = P(expression1 | expression2)

  private def booleanOperator[$: P]: P[BinaryBooleanOperator] =
    P(P("&&").map(_ => AndOperator) | P("||").map(_ => OrOperator))

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def booleanExpression[$: P]: P[FilterToken] =
    P(expression ~ (booleanOperator ~ booleanExpression).?).map {
      case (lhs, None) => lhs
      // Balance the AST tree so that all "Or" operations are always on top of any "And" operation.
      // Indeed, the "And" operations have a higher priority and must be executed first.
      case (lhs1, Some((AndOperator, BooleanFilter(OrOperator, lhs2, rhs2)))) =>
        BooleanFilter(OrOperator, BooleanFilter(AndOperator, lhs1, lhs2), rhs2)
      case (lhs, Some((op, rhs))) => BooleanFilter(op, lhs, rhs)
    }

  private def recursiveSubscriptFilter[$: P]: P[RecursiveFilterToken] =
    P(("..*" | "..") ~ subscriptFilter).map(RecursiveFilterToken)

  private[jsonpath] def subscriptFilter[$: P]: P[FilterToken] =
    P("[?(" ~ booleanExpression ~ ")]")

  /// child accessors parsers ///////////////////////////////////////////////

  private[jsonpath] def subscriptField[$: P]: P[FieldAccessor] =
    P("[" ~ quotedField.rep(1, ",") ~ "]").map {
      case f1 :: Nil => Field(f1)
      case fields    => MultiField(fields.toList)
    }

  private[jsonpath] def dotField[$: P]: P[FieldAccessor] =
    P("." ~ field).map(Field)

  // TODO recursive with `subscriptField`
  private def recursiveField[$: P]: P[FieldAccessor] =
    P(".." ~ field).map(RecursiveField)

  private def anyChild[$: P]: P[FieldAccessor] =
    P(".*" | "['*']" | """["*"]""").map(_ => AnyField)

  private def recursiveAny[$: P]: P[FieldAccessor] = P("..*").map(_ => RecursiveAnyField)

  private[jsonpath] def fieldAccessors[$: P]: P[PathToken] = P(
    dotField
      | recursiveSubscriptFilter
      | recursiveAny
      | recursiveField
      | anyChild
      | subscriptField
  )

  /// Main parsers //////////////////////////////////////////////////////////

  private def childAccess[$: P] = P(fieldAccessors | arrayAccessors)

  private[jsonpath] def pathSequence[$: P]: P[List[PathToken]] = P(
    (childAccess | subscriptFilter).rep
  ).map(_.toList)

  private[jsonpath] def root[$: P]: P[PathToken] = P("$").map(_ => RootNode)

  private def query[$: P]: P[List[PathToken]] =
    P(Start ~ root ~ pathSequence ~ End).map { case (r, ps) => r :: ps }

  private[jsonpath] def parse[A](
      rule: P[_] => P[A],
      jsonpath: String
  ): Parsed[A] = fastparse.parse(jsonpath, rule)

  private[jsonpath] def parse(jsonpath: String): Parsed[List[PathToken]] =
    parse(JsonPathParser.query(_), jsonpath)
}

private[jsonpath] final class JsonPathParser {
  def compile(jsonpath: String): Either[JPError, JsonPath] = JsonPathParser.parse(
    jsonpath
  ) match {
    case Parsed.Success(q, _) => Right(new JsonPath(q))
    case ns: Parsed.Failure   => Left(JPError(ns.msg))
  }
}
