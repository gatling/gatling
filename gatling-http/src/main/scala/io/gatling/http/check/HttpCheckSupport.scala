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
package io.gatling.http.check

import io.gatling.core.session.Expression
import io.gatling.http.check.body._
import io.gatling.http.check.checksum.HttpChecksumCheckBuilder
import io.gatling.http.check.header.{ HttpHeaderCheckBuilder, HttpHeaderRegexCheckBuilder }
import io.gatling.http.check.status.HttpStatusCheckBuilder
import io.gatling.http.check.time.HttpResponseTimeCheckBuilder
import io.gatling.http.check.url.CurrentLocationCheckBuilder

trait HttpCheckSupport {

  val regex = HttpBodyRegexCheckBuilder.regex _

  def xpath(expression: Expression[String], namespaces: List[(String, String)] = Nil) = HttpBodyXPathCheckBuilder.xpath(expression, namespaces)

  def css(selector: Expression[String]) = HttpBodyCssCheckBuilder.css(selector, None)
  def css(selector: Expression[String], nodeAttribute: String) = HttpBodyCssCheckBuilder.css(selector, Some(nodeAttribute))

  val jsonPath = HttpBodyJsonPathCheckBuilder.jsonPath _
  val jsonpJsonPath = HttpBodyJsonpJsonPathCheckBuilder.jsonpJsonPath _

  val bodyString = HttpBodyStringCheckBuilder.BodyString
  val bodyBytes = HttpBodyBytesCheckBuilder.BodyBytes

  val header = HttpHeaderCheckBuilder.header _

  val headerRegex = HttpHeaderRegexCheckBuilder.headerRegex _

  val status = HttpStatusCheckBuilder.Status

  val currentLocation = CurrentLocationCheckBuilder.CurrentLocation

  val md5 = HttpChecksumCheckBuilder.Md5
  val sha1 = HttpChecksumCheckBuilder.Sha1

  val responseTimeInMillis = HttpResponseTimeCheckBuilder.ResponseTimeInMillis
  val latencyInMillis = HttpResponseTimeCheckBuilder.LatencyInMillis
}
