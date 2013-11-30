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

import scala.util.Try
import io.gatling.recorder.util.Json
import java.io.InputStream
import org.joda.convert.StringConvert.{ INSTANCE => convertInstance }
import org.joda.time.DateTime

object HarMapping {

	def jsonToHttpArchive(json: Json): HttpArchive =
		HttpArchive(buildLog(json.log))

	private def parseMillisFromIso8601DateTime(time: String): Long =
		convertInstance.convertFromString(classOf[DateTime], time).getMillis

	private def buildLog(log: Json) = {
		val startedTimestamps = log.entries.map(e => parseMillisFromIso8601DateTime(e.startedDateTime))
		val initTime = startedTimestamps.headOption.getOrElse(0l)
		val timeBetweenRequests = startedTimestamps.zip(initTime +: startedTimestamps).map { case (t2, t1) => t2 - t1 }

		val entries = timeBetweenRequests.zip(log.entries)
			.map { case (lag, entry) => Entry(lag, buildRequest(entry.request), buildResponse(entry.response)) }

		Log(entries)
	}

	private def buildEntry(entry: Json): Entry =
		Entry(parseMillisFromIso8601DateTime(entry.startedDateTime),
			buildRequest(entry.request), buildResponse(entry.response))

	private def buildRequest(request: Json) = {
		// FIXME : try early resolution of postData, to trigger the exception
		val postData = Try(request.postData.toString).toOption.map(_ => request.postData)
		Request(request.method, request.url, request.headers.map(buildHeader), postData.map(buildPostData))
	}

	private val protectedValue = """"(.*)\"""".r
	private def unprotected(string: String) = string match {
		case protectedValue(unprotected) => unprotected
		case _ => string
	}

	private def buildResponse(response: Json) = Response(response.status)

	private def buildHeader(header: Json) = Header(header.name, unprotected(header.value))

	private def buildPostData(postData: Json) = PostData(postData.mimeType, postData.text, postData.params.map(buildPostParam))

	private def buildPostParam(postParam: Json) = PostParam(postParam.name, unprotected(postParam.value))
}

/*
 * HAR mapping is incomplete, as we deserialize only what is strictly necessary for building a simulation
 */
case class HttpArchive(log: Log)

case class Log(entries: Seq[Entry])

case class Entry(lag: Long, request: Request, response: Response)

case class Request(method: String, url: String, headers: Seq[Header], postData: Option[PostData])

case class Response(status: Int)

case class Header(name: String, value: String)

case class PostData(mimeType: String, text: String, params: Seq[PostParam])

case class PostParam(name: String, value: String)
