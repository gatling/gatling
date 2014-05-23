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
import java.net.{ URI, URL }
import scala.collection.breakOut
import scala.util.Try
import org.jboss.netty.handler.codec.http.HttpMethod

import io.gatling.core.util.IO
import io.gatling.core.util.StringHelper.RichString
import io.gatling.core.util.StandardCharsets.UTF_8
import io.gatling.http.HeaderNames.CONTENT_TYPE
import io.gatling.http.fetch.HtmlParser
import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.recorder.model._
import io.gatling.recorder.util.Json

/**
 * Implementation according to http://www.softwareishard.com/blog/har-12-spec/
 */
object HarReader extends IO {

  def apply(path: String)(implicit config: RecorderConfiguration): SimulationModel =
    withCloseable(new FileInputStream(path))(apply(_))

  def apply(jsonStream: InputStream)(implicit config: RecorderConfiguration): SimulationModel =
    apply(Json.parseJson(jsonStream))

  def apply(json: Json)(implicit config: RecorderConfiguration): SimulationModel = {
    val HttpArchive(Log(entries)) = HarMapping.jsonToHttpArchive(json)

    implicit val model = new SimulationModel()

    val times = entries.iterator
      .filter(e =>
        e.request.method != HttpMethod.CONNECT.getName)
      .filter(e =>
        isValidURL(e.request.url))
      // TODO NICO : can't we move this in Scenario as well ?
      .filter(e => {
        val r = config.filters.filters.map(_.accept(e.request.url)).getOrElse(true)
        r
      })
      .map { createRequestWithArrivalTime }

    if (times.hasNext)
      times.max // force evaluate - TODO there is likely a "right" way of doing this

    model.postProcess
    model
  }

  private def createRequestWithArrivalTime(entry: Entry)(implicit model: SimulationModel): Long = {

      def buildContent(postParams: Seq[PostParam]): RequestBodyModel =
        RequestBodyParams(postParams.map(postParam => (postParam.name, postParam.value)).toList)

    val uri = entry.request.url
    val method = entry.request.method
    val headers = buildHeaders(entry)

    // NetExport doesn't copy post params to text field
    val body = entry.request.postData.flatMap { postData =>
      if (!postData.params.isEmpty)
        Some(buildContent(postData.params))
      else
        // HAR files are required to be saved in UTF-8 encoding, other encodings are forbidden
        postData.text.trimToOption.map(text => RequestBodyBytes(text.getBytes(UTF_8)))
    }

    val embeddedResources = entry.response.content match {
      case Content("text/html", Some(text)) => HtmlParser.getEmbeddedResources(new URI(uri), text.toCharArray)
      case _                                => Nil
    }

    val requestModel = RequestModel(uri, method, headers, body, entry.response.status, embeddedResources, /*responseContentType : Option[String]*/ None)
    //notify the model
    model += (entry.arrivalTime, requestModel)

    entry.arrivalTime

  }

  private def buildHeaders(entry: Entry): Map[String, String] = {
    // Chrome adds extra headers, eg: ":host". We should have them in the Gatling scenario.
    // TODO - get a HAR file that shows this into the fixtures
    val headers: Map[String, String] = entry.request.headers.filter(!_.name.startsWith(":")).map(h => (h.name, h.value))(breakOut)

    // NetExport doesn't add Content-Type to headers when POSTing, but both Chrome Dev Tools and NetExport set mimeType
    // TODO - get a HAR file that shows this into the fixtures
    entry.request.postData.map(postData => headers.updated(CONTENT_TYPE, postData.mimeType)).getOrElse(headers)
  }

  private def isValidURL(url: String): Boolean = Try(new URL(url)).isSuccess
}
