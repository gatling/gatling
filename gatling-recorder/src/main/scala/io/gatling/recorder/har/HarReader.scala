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
package io.gatling.recorder.har

import java.io.{ FileInputStream, InputStream }
import java.net.URL

import scala.concurrent.duration.DurationLong
import scala.util.Try

import io.gatling.core.util.IOHelper
import io.gatling.core.util.StringHelper.RichString
import io.gatling.http.HeaderNames.CONTENT_TYPE
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.scenario.{ PauseElement, RequestBodyBytes, RequestBodyParams, RequestElement, ScenarioElement }
import io.gatling.recorder.util.Json
import io.gatling.recorder.util.RedirectHelper.isRequestRedirect

object HarReader {

	def apply(path: String): List[ScenarioElement] =
		IOHelper.withCloseable(new FileInputStream(path))(apply(_))

	def apply(jsonStream: InputStream): List[ScenarioElement] =
		apply(Json.parseJson(jsonStream))

	def apply(json: Json): List[ScenarioElement] = {
		val HttpArchive(Log(entries)) = HarMapping.jsonToHttpArchive(json)
		entries.iterator
			.filter(e => isValidURL(e.request.url))
			// MOVE THIS OUT - we want a sequence of filters ... 
			// the fetch resources need to be applied here too.
			.filter(e => configuration.filters.filters.map(_.accept(e.request.url)).getOrElse(true))
			.flatMap(createScenarioElements(_))
			.toList
	}

	private def createScenarioElements(entry: Entry): List[ScenarioElement] = {
		if (isRequestRedirect(entry.response.status)) {
			List()
		} else {
			val req = createRequest(entry)
			// TODO we should have a configuration for that.
			if (entry.lag <= 10) {
				List(req)
			} else
				List(new PauseElement(entry.lag milliseconds), req)
		}
	}

	private def createRequest(entry: Entry): RequestElement = {
		def buildContent(postParams: Seq[PostParam]) =
			RequestBodyParams(postParams.map(postParam => (postParam.name, postParam.value)).toList)

		val uri = entry.request.url
		val method = entry.request.method
		val headers = buildHeaders(entry)

		// NetExport doesn't copy post params to text field
		val body = entry.request.postData.map { postData =>
			postData.text.trimToOption match {
				case Some(string) => RequestBodyBytes(string.getBytes(configuration.core.encoding))
				case None => buildContent(postData.params)
			}
		}

		RequestElement(uri, method, headers, body, entry.response.status, None)
	}

	private def buildHeaders(entry: Entry): Map[String, String] = {
		val headers = entry.request.headers.map(h => (h.name, h.value)).toMap
		// NetExport doesn't add Content-Type to headers when POSTing, but both Chrome Dev Tools and NetExport set mimeType
		entry.request.postData.map(postData => headers.updated(CONTENT_TYPE, postData.mimeType)).getOrElse(headers)
	}

	private def isValidURL(url: String): Boolean = Try(new URL(url)).isSuccess
}
