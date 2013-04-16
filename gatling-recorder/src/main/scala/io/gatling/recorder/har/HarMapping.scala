/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

case class HttpArchive(log: Log)

@JsonIgnoreProperties(ignoreUnknown = true)
case class Log(entries: Seq[Entry])

@JsonIgnoreProperties(ignoreUnknown = true)
case class Entry(startedDateTime: String, request: Request, response: Response)

@JsonIgnoreProperties(ignoreUnknown = true)
case class Request(method: String, url: String, headers: Seq[Header], queryString: Seq[QueryParam], postData: Option[PostData])

@JsonIgnoreProperties(ignoreUnknown = true)
case class Response(status: Int, headers: Seq[Header], content: Content, redirectURL: String)

@JsonIgnoreProperties(ignoreUnknown = true)
case class Header(name: String, value: String)

@JsonIgnoreProperties(ignoreUnknown = true)
case class QueryParam(name: String, value: String)

@JsonIgnoreProperties(ignoreUnknown = true)
case class PostData(mimeType: String, text: String, params: Seq[PostParam])

@JsonIgnoreProperties(ignoreUnknown = true)
case class PostParam(name: String, value: Option[String], fileName: Option[String], contentType: Option[String])

@JsonIgnoreProperties(ignoreUnknown = true)
case class Content(size: Int, text: Option[String])