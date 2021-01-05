/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.io.InputStream

import scala.jdk.CollectionConverters._

import com.fasterxml.jackson.databind.{ DeserializationFeature, ObjectMapper }

object HarParser {

  final case class HarEntry(startedDateTime: String, time: Option[Double], timings: Option[HarTimings], request: HarRequest, response: HarResponse)

  final case class HarRequest(httpVersion: String, method: String, url: String, headers: Seq[HarHeader], postData: Option[HarRequestPostData])

  final case class HarHeader(name: String, value: String)

  final case class HarRequestPostData(text: Option[String], params: Seq[HarRequestPostParam])

  final case class HarRequestPostParam(name: String, value: String)

  final case class HarResponse(status: Int, headers: Seq[HarHeader], statusText: String, content: HarResponseContent)

  final case class HarResponseContent(mimeType: Option[String], encoding: Option[String], text: Option[String], comment: Option[String])

  final case class HarTimings(blocked: Double, dns: Double, connect: Double, ssl: Double, send: Double, waitTiming: Double, receive: Double) {
    val time: Double = blocked + dns + connect + ssl + send + waitTiming + receive
  }

  private val TheObjectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def parseHarEntries(is: InputStream): Seq[HarEntry] = {
    val javaModel = TheObjectMapper.readValue(is, classOf[HarJavaModel.HarHttpArchive])

    Option(javaModel.getLog.getEntries)
      .map {
        _.asScala.map { entry =>
          HarEntry(
            startedDateTime = entry.getStartedDateTime,
            time = Option(entry.getTime),
            timings = Option(entry.getTimings).map(timings =>
              HarTimings(
                blocked = timings.getBlocked,
                dns = timings.getDns,
                connect = timings.getConnect,
                ssl = timings.getSsl,
                send = timings.getSend,
                waitTiming = timings.getWait,
                receive = timings.getReceive
              )
            ),
            request = HarRequest(
              httpVersion = entry.getRequest.getHttpVersion,
              method = entry.getRequest.getMethod,
              url = entry.getRequest.getUrl,
              headers = Option(entry.getRequest.getHeaders)
                .map(
                  _.asScala
                    .map(header =>
                      HarHeader(
                        name = header.getName,
                        value = header.getValue
                      )
                    )
                    .toSeq
                )
                .getOrElse(Nil),
              postData = Option(entry.getRequest.getPostData).map(postData =>
                HarRequestPostData(
                  text = Option(postData.getText),
                  params = Option(postData.getParams)
                    .map(
                      _.asScala
                        .map(param =>
                          HarRequestPostParam(
                            name = param.getName,
                            value = param.getValue
                          )
                        )
                        .toSeq
                    )
                    .getOrElse(Nil)
                )
              )
            ),
            response = HarResponse(
              status = entry.getResponse.getStatus,
              headers = Option(entry.getResponse.getHeaders)
                .map(
                  _.asScala
                    .map(header =>
                      HarHeader(
                        name = header.getName,
                        value = header.getValue
                      )
                    )
                    .toSeq
                )
                .getOrElse(Nil),
              statusText = entry.getResponse.getStatusText,
              content = HarResponseContent(
                mimeType = Option(entry.getResponse.getContent.getMimeType),
                encoding = Option(entry.getResponse.getContent.getEncoding),
                text = Option(entry.getResponse.getContent.getText),
                comment = Option(entry.getResponse.getContent.getComment)
              )
            )
          )
        }.toSeq
      }
      .getOrElse(Nil)
  }
}
