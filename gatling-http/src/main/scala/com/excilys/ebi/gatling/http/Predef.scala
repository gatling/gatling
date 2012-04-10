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
package com.excilys.ebi.gatling.http
import com.excilys.ebi.gatling.core.session.EvaluatableString
import com.excilys.ebi.gatling.http.check.body.{ HttpBodyXPathCheckBuilder, HttpBodyRegexCheckBuilder, HttpBodyJsonPathCheckBuilder }
import com.excilys.ebi.gatling.http.check.header.HttpHeaderCheckBuilder
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheckBuilder
import com.excilys.ebi.gatling.http.config.{ HttpProxyBuilder, HttpProtocolConfigurationBuilder }
import com.excilys.ebi.gatling.http.request.builder.HttpRequestBaseBuilder

object Predef {

	def http(requestName: String) = HttpRequestBaseBuilder.http(requestName)

	def httpConfig = HttpProtocolConfigurationBuilder.httpConfig
	implicit def toHttpProtocolConfiguration(hpb: HttpProxyBuilder) = HttpProxyBuilder.toHttpProtocolConfiguration(hpb)
	implicit def toHttpProtocolConfiguration(builder: HttpProtocolConfigurationBuilder) = HttpProtocolConfigurationBuilder.toHttpProtocolConfiguration(builder)

	def regex(expression: EvaluatableString) = HttpBodyRegexCheckBuilder.regex(expression)
	def xpath(expression: EvaluatableString, namespaces: List[(String, String)] = Nil) = HttpBodyXPathCheckBuilder.xpath(expression, namespaces)
	def jsonPath(expression: EvaluatableString) = HttpBodyJsonPathCheckBuilder.jsonPath(expression)
	def header(expression: EvaluatableString) = HttpHeaderCheckBuilder.header(expression)
	def status = HttpStatusCheckBuilder.status
}