/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.recorder.har

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat

import scala.util.Try

import io.gatling.commons.util.StringHelper.RichString
import io.gatling.recorder.har.Json.{ JsonToInt, JsonToString }

import org.asynchttpclient.util.Base64

private[har] object HarMapping {

  private val ProtectedValue = """"(.*)\"""".r

  // HAR files are required to be saved in UTF-8 encoding, other encodings are forbidden
  val Charset = StandardCharsets.UTF_8

  def jsonToHttpArchive(json: Json): HttpArchive = HttpArchive(buildLog(json.log))

  // Correctly parse the date when the millisecond field contains more than three digits
  // IOW, fix this error:
  // java.text.ParseException: Unparseable date: "2016-06-16T13:22:09.1137657-05:00"
  //     at java.text.DateFormat.parse(DateFormat.java:366)
  //     at io.gatling.recorder.har.HarMapping$.parseMillisFromIso8601DateTime(HarMapping.scala:38)
  //     ...
  private def parseMillisFromIso8601DateTime(time: String): Long = java.time.ZonedDateTime.parse(time).toInstant().toEpochMilli

  private def buildLog(log: Json): Log = {
    val entries = log.entries.iterator
      // Filter out all non-HTTP protocols (eg: ws://)
      .filter(_.request.url.toString.toLowerCase.startsWith("http"))
      // Filter out all HTTP request with status=0, http://www.w3.org/TR/XMLHttpRequest/#the-status-attribute
      .filter(_.response.status.toInt != 0)
      .map(buildEntry)
      .toVector

    Log(entries)
  }

  private def buildEntry(entry: Json): Entry = {
    val startTime = parseMillisFromIso8601DateTime(entry.startedDateTime)
    // what a thing of beauty!!!
    val time = Try(entry.time.toLong).getOrElse(entry.time.toDouble.toLong)
    Entry(
      startTime,
      startTime + time,
      buildRequest(entry.request), buildResponse(entry.response)
    )
  }

  private def buildRequest(request: Json): Request = {
    val postData = request.postData.toOption
    Request(request.method, request.url, request.headers.map(buildHeader), postData.map(buildPostData))
  }
  private def unprotected(string: String) = string match {
    case ProtectedValue(unprotected) => unprotected
    case _                           => string
  }

  private def buildResponse(response: Json) = {
    val mimeType = response.content.mimeType
    assert(mimeType.toOption.isDefined, s"Response content ${response.content} does not contains a mimeType")

    val rawText = response.content.text.toOption.map(_.toString.trim).filter(!_.isEmpty)
    val encoding = response.content.encoding.toOption.map(_.toString.trim)
    val text = rawText flatMap (_.trimToOption) map { trimmedText =>
      encoding match {
        case Some("base64") => new String(Base64.decode(trimmedText), HarMapping.Charset)
        case _              => trimmedText
      }
    }

    val content = Content(mimeType, text)
    Response(response.status, content)
  }

  private def buildHeader(header: Json) = Header(header.name, unprotected(header.value))

  private def buildPostData(postData: Json) = PostData(postData.mimeType, postData.text, postData.params.map(buildPostParam))

  private def buildPostParam(postParam: Json) = PostParam(postParam.name, unprotected(postParam.value))
}

/*
 * HAR mapping is incomplete, as we deserialize only what is strictly necessary for building a simulation
 */
private[har] case class HttpArchive(log: Log)

private[har] case class Log(exchanges: Seq[Entry])

private[har] case class Entry(sendTime: Long, arrivalTime: Long, request: Request, response: Response)

private[har] case class Request(method: String, url: String, headers: Seq[Header], postData: Option[PostData])

private[har] case class Response(status: Int, content: Content)

private[har] case class Content(mimeType: String, text: Option[String]) {
  def textAsBytes = text.map(_.getBytes(HarMapping.Charset))
}

private[har] case class Header(name: String, value: String)

private[har] case class PostData(mimeType: String, text: String, params: Seq[PostParam]) {
  def textAsBytes = text.trimToOption.map(_.getBytes(HarMapping.Charset))
}

private[har] case class PostParam(name: String, value: String)
