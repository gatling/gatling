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

import java.net.URL

import scala.concurrent.duration.DurationLong
import scala.io.Source
import scala.util.Try

import org.joda.convert.StringConvert
import org.joda.time.DateTime

import io.gatling.core.util.StringHelper.RichString
import io.gatling.http.HeaderNames.CONTENT_TYPE
import io.gatling.recorder.config.RecorderConfiguration.configuration
import io.gatling.recorder.scenario.{ PauseElement, RequestBodyBytes, RequestBodyParams, RequestElement, ScenarioElement }
import io.gatling.recorder.util.RedirectHelper.isRequestRedirect

object HarReader {

	var scenarioElements: List[ScenarioElement] = Nil

	private var lastEntry: Entry = _
	private var lastRequestTimestamp: Long = 0

	def processHarFile(path: String) {
		def isValidURL(url: String) = Try(new URL(url)).isSuccess

		val json = Source.fromFile(path).getLines.mkString
		val httpArchive = HarMapping.jsonToHttpArchive(json)
		httpArchive.log.entries.filter(entry => isValidURL(entry.request.url)).foreach(processEntry)

	}

	def cleanHarReaderState {
		scenarioElements = Nil
		lastEntry = null
		lastRequestTimestamp = 0
	}

	private def processEntry(entry: Entry) {

		def buildHeaders(entry: Entry) = {
			val headers = entry.request.headers.map(header => (header.name, header.value)).toMap
			// NetExport doesn't add Content-Type to headers when POSTing, but both Chrome Dev Tools and NetExport set mimeType
			entry.request.postData.map(postData => headers.updated(CONTENT_TYPE, postData.mimeType)).getOrElse(headers)
		}

		def createRequest(entry: Entry, statusCode: Int) {
			def buildContent(postParams: Seq[PostParam]) = RequestBodyParams(postParams.map(postParam => (postParam.name, postParam.value)).toList)

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
			scenarioElements = RequestElement(uri, method, headers, body, statusCode, None) :: scenarioElements
		}

		def createPause {
			def parseMillisFromIso8601DateTime(time: String) = StringConvert.INSTANCE.convertFromString(classOf[DateTime], time).getMillis

			val timestamp = parseMillisFromIso8601DateTime(entry.startedDateTime)
			val diff = timestamp - lastRequestTimestamp
			if (lastRequestTimestamp != 0 && diff > 10) {
				scenarioElements = new PauseElement(diff milliseconds) :: scenarioElements
			}
			lastRequestTimestamp = timestamp
		}

		if (configuration.filters.filters.map(_.accept(entry.request.url)).getOrElse(true)) {
			if (lastEntry == null && isRequestRedirect(entry.response.status)) {
				createPause
				lastEntry = entry

			} else if (lastEntry != null && !isRequestRedirect(entry.response.status)) {
				// process request with new status
				createRequest(lastEntry, entry.response.status)
				lastEntry = null

			} else if (lastEntry == null) {
				// standard use case
				createPause
				createRequest(entry, entry.response.status)
			}
		}
	}
}
