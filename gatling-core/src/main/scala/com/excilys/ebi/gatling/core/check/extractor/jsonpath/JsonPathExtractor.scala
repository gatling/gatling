/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.check.extractor.jsonpath
import scala.annotation.tailrec
import scala.collection.immutable.Stack

import org.codehaus.jackson.{ JsonToken, JsonParser, JsonFactory }

import com.excilys.ebi.gatling.core.check.extractor.Extractor.{ toOption, seqToOption }
import com.excilys.ebi.gatling.core.check.extractor.jsonpath.JsonPathExtractor.FACTORY
import com.excilys.ebi.gatling.core.check.extractor.jsonpath.JsonPathTokenizer.{ unstack, tokenize }
import com.excilys.ebi.gatling.core.util.IOHelper.use

object JsonPathExtractor {
	lazy val FACTORY = new JsonFactory
}

class JsonPathExtractor(textContent: String) {

	@tailrec
	private def walkRec(expectedPath: List[JsonPathElement], parser: JsonParser, stack: Stack[JsonPathElement], stackAsList: Option[List[JsonPathElement]], unstackedExpectedPath: Option[List[JsonPathElement]], depth: Int, results: List[String]): List[String] = {

		def handleStartArray: (Stack[JsonPathElement], Option[List[JsonPathElement]], Option[List[JsonPathElement]], Int, List[String]) = (stack.push(ArrayElementNode(parser.getCurrentName, 0)), None, None, depth + 1, results)

		def handleStartObject: (Stack[JsonPathElement], Option[List[JsonPathElement]], Option[List[JsonPathElement]], Int, List[String]) = {
			val head = if (stack.isEmpty) None else Some(stack.head)
			val newStack = head match {
				case Some(ArrayElementNode(name, index)) => stack.pop.push(ArrayElementNode(name, index + 1))
				case _ => stack.push(SimpleNode(parser.getCurrentName))
			}
			(newStack, None, None, depth + 1, results)
		}

		def handleEndArray: (Stack[JsonPathElement], Option[List[JsonPathElement]], Option[List[JsonPathElement]], Int, List[String]) = (stack.pop, None, None, depth - 1, results)

		def handleEndObject: (Stack[JsonPathElement], Option[List[JsonPathElement]], Option[List[JsonPathElement]], Int, List[String]) = {
			val head = if (stack.isEmpty) None else Some(stack.head)
			val newStack = head match {
				case Some(ArrayElementNode(name, index)) => stack
				case Some(_) => stack.pop
				case None => stack
			}
			(newStack, None, None, depth - 1, results)
		}

		def handleFieldName: (Stack[JsonPathElement], Option[List[JsonPathElement]], Option[List[JsonPathElement]], Int, List[String]) = (stack, stackAsList, unstackedExpectedPath, depth, results)

		def handleValue: (Stack[JsonPathElement], Option[List[JsonPathElement]], Option[List[JsonPathElement]], Int, List[String]) = {
			val newStackAsList = stackAsList.getOrElse(stack.toList)
			val actualPath = SimpleNode(parser.getCurrentName) :: newStackAsList
			val newUnstackedExpectedPath = unstackedExpectedPath.getOrElse(unstack(expectedPath, actualPath.length))
			val newResults = if (JsonPathMatcher.matchPath(newUnstackedExpectedPath, actualPath)) parser.getText :: results else results
			(stack, Some(newStackAsList), Some(newUnstackedExpectedPath), depth, newResults)
		}

		if (depth == 0)
			results
		else {
			val (newStack, newStackAsList, newUnstackedExpectedPath, newDepth, newResults) = parser.nextToken match {
				case JsonToken.START_ARRAY => handleStartArray
				case JsonToken.START_OBJECT => handleStartObject
				case JsonToken.END_ARRAY => handleEndArray
				case JsonToken.END_OBJECT => handleEndObject
				case JsonToken.FIELD_NAME => handleFieldName
				case _ => handleValue
			}
			walkRec(expectedPath, parser, newStack, newStackAsList, newUnstackedExpectedPath, newDepth, newResults)
		}
	}

	def extractOne(occurrence: Int)(expression: String): Option[String] = extractMultiple(expression) match {
		case Some(results) if (results.length > occurrence) => results(occurrence)
		case _ => None
	}

	def extractMultiple(expression: String): Option[Seq[String]] = use(FACTORY.createJsonParser(textContent)) { parser =>
		val expected = tokenize(expression)
		parser.nextToken
		val results = walkRec(expected, parser, Stack[JsonPathElement](), None, None, 1, Nil)
		results.reverse
	}

	def count(expression: String): Option[Int] = extractMultiple(expression).map(_.size)
}