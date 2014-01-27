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
package io.gatling.recorder.har

import java.io.{ FileInputStream, InputStream }
import java.net.URL

import scala.util.Try

import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.core.util.StringHelper.RichString
import io.gatling.http.HeaderNames.CONTENT_TYPE
import io.gatling.http.fetch.HtmlParser
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.scenario.{ RequestBodyBytes, RequestBodyParams, RequestElement, ScenarioDefinition }
import io.gatling.recorder.util.Json

/**
 * Implementation according to http://www.softwareishard.com/blog/har-12-spec/
 */
object HarReader {

	def apply(path: String)(implicit config: RecorderConfiguration): ScenarioDefinition =
		withCloseable(new FileInputStream(path))(apply(_))

	def apply(jsonStream: InputStream)(implicit config: RecorderConfiguration): ScenarioDefinition =
		apply(Json.parseJson(jsonStream))

	def apply(json: Json)(implicit config: RecorderConfiguration): ScenarioDefinition = {
		val HttpArchive(Log(entries)) = HarMapping.jsonToHttpArchive(json)

		val elements = entries.iterator
			.filter(e => isValidURL(e.request.url))
			// TODO NICO : can't we move this in Scenario as well ?
			.filter(e => config.filters.filters.map(_.accept(e.request.url)).getOrElse(true))
			.map(createRequestWithArrivalTime)
			.toVector

		ScenarioDefinition(elements, Nil)
	}

	private def createRequestWithArrivalTime(entry: Entry) = {
		def buildContent(postParams: Seq[PostParam]) =
			RequestBodyParams(postParams.map(postParam => (postParam.name, postParam.value)).toList)

		val uri = entry.request.url
		val method = entry.request.method
		val headers = buildHeaders(entry)

		// NetExport doesn't copy post params to text field
		val body = entry.request.postData.map { postData =>
			postData.text.trimToOption match {
				// HAR files are required to be saved in UTF-8 encoding, other encodings are forbidden
				case Some(string) => RequestBodyBytes(string.getBytes("UTF-8"))
				case None => buildContent(postData.params)
			}
		}

		val embeddedResources = entry.response.content match {
			case Content("text/html", Some(text)) =>
				HtmlParser.getEmbeddedResources(new java.net.URI(uri), text.toCharArray)
			case _ => Nil
		}

		(entry.arrivalTime, RequestElement(uri, method, headers, body, entry.response.status, embeddedResources))
	}

	private def buildHeaders(entry: Entry): Map[String, String] = {
		val headers = entry.request.headers.map(h => (h.name, h.value)).toMap
		// NetExport doesn't add Content-Type to headers when POSTing, but both Chrome Dev Tools and NetExport set mimeType
		entry.request.postData.map(postData => headers.updated(CONTENT_TYPE, postData.mimeType)).getOrElse(headers)
	}

	private def isValidURL(url: String): Boolean = Try(new URL(url)).isSuccess
}
