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

package io.gatling.http.check

import java.nio.charset.StandardCharsets.UTF_8

import scala.xml.Elem

import io.gatling.http.MissingNettyHttpHeaderValues
import io.gatling.http.response.{ ByteArrayResponseBody, Response, ResponseBody, StringResponseBody }

import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpHeaderNames, HttpResponseStatus }

package object body {

  def mockResponse(body: String): Response =
    mockResponse(new StringResponseBody(body, UTF_8))

  def mockResponse(body: Array[Byte]): Response =
    mockResponse(new ByteArrayResponseBody(body, UTF_8))

  def mockResponse(xml: Elem): Response =
    mockResponse(xml.toString).copy(
      headers = new DefaultHttpHeaders()
        .add(HttpHeaderNames.CONTENT_TYPE, s"${MissingNettyHttpHeaderValues.ApplicationXml}; charset=$UTF_8")
    )

  def mockResponse(body: ResponseBody): Response =
    Response(
      request = null,
      status = HttpResponseStatus.OK,
      headers = new DefaultHttpHeaders,
      body = body,
      checksums = null,
      startTimestamp = 0,
      endTimestamp = 0,
      isHttp2 = false
    )
}
