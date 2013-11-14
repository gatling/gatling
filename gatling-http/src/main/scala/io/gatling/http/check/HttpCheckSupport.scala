/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.check

import io.gatling.core.check.extractor.jsonpath.JsonFilter
import io.gatling.core.check.extractor.regex.GroupExtractor
import io.gatling.core.session.Expression
import io.gatling.http.check.body.{ HttpBodyCssCheckBuilder, HttpBodyJsonPathCheckBuilder, HttpBodyJsonpJsonPathCheckBuilder, HttpBodyRegexCheckBuilder, HttpBodyStringCheckBuilder, HttpBodyXPathCheckBuilder }
import io.gatling.http.check.checksum.HttpChecksumCheckBuilder
import io.gatling.http.check.header.{ HttpHeaderCheckBuilder, HttpHeaderRegexCheckBuilder }
import io.gatling.http.check.status.HttpStatusCheckBuilder
import io.gatling.http.check.time.HttpResponseTimeCheckBuilder
import io.gatling.http.check.url.CurrentLocationCheckBuilder

trait HttpCheckSupport {

	val regex = regexTyped[String] _
	def regexTyped[T](pattern: Expression[String])(implicit groupExtractor: GroupExtractor[T]) = HttpBodyRegexCheckBuilder.regex[T](pattern)

	def xpath(expression: Expression[String], namespaces: List[(String, String)] = Nil) = HttpBodyXPathCheckBuilder.xpath(expression, namespaces)

	def css(selector: Expression[String]) = HttpBodyCssCheckBuilder.css(selector, None)
	def css(selector: Expression[String], nodeAttribute: String) = HttpBodyCssCheckBuilder.css(selector, Some(nodeAttribute))

	val jsonPath = jsonPathTyped[String] _
	def jsonPathTyped[T](path: Expression[String])(implicit groupExtractor: JsonFilter[T]) = HttpBodyJsonPathCheckBuilder.jsonPath[T](path)
	val jsonpJsonPath = jsonpJsonPathTyped[String] _
	def jsonpJsonPathTyped[T](path: Expression[String])(implicit groupExtractor: JsonFilter[T]) = HttpBodyJsonpJsonPathCheckBuilder.jsonpJsonPath[T](HttpBodyJsonpJsonPathCheckBuilder.JSONP_REGEX, path)

	val bodyString = HttpBodyStringCheckBuilder.bodyString

	val header = HttpHeaderCheckBuilder.header _

	val headerRegex = headerRegexTyped[String] _
	def headerRegexTyped[T](headerName: Expression[String], pattern: Expression[String])(implicit groupExtractor: GroupExtractor[T]) = HttpHeaderRegexCheckBuilder.headerRegex[T](headerName, pattern)

	val status = HttpStatusCheckBuilder.status

	val currentLocation = CurrentLocationCheckBuilder.currentLocation

	val md5 = HttpChecksumCheckBuilder.md5
	val sha1 = HttpChecksumCheckBuilder.sha1

	val responseTimeInMillis = HttpResponseTimeCheckBuilder.responseTimeInMillis
	val latencyInMillis = HttpResponseTimeCheckBuilder.latencyInMillis
}
