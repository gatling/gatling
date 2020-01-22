/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.http.engine

import io.gatling.core.CoreComponents
import io.gatling.http.client.HttpListener
import io.gatling.http.engine.response.ResponseProcessor
import io.gatling.http.engine.tx.HttpTx

import com.typesafe.scalalogging._
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }

/**
 * This class is the AsyncHandler that AsyncHttpClient needs to process a request's response
 *
 * It is part of the HttpRequestAction
 */
class GatlingHttpListener(tx: HttpTx, coreComponents: CoreComponents, responseProcessor: ResponseProcessor) extends HttpListener with LazyLogging {

  private val responseBuilder = tx.responseBuilderFactory(tx.request.clientRequest)
  private var init = false
  private var done = false
  // [fl]
  //
  //
  //
  //
  //
  // [fl]

  override def onSend(): Unit =
    if (!init) {
      init = true
      responseBuilder.updateStartTimestamp()
      // [fl]
      //
      //
      //
      // [fl]
    }

  // [fl]
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  // [fl]

  override def onHttpResponse(status: HttpResponseStatus, headers: HttpHeaders): Unit =
    if (!done) {
      responseBuilder.accumulate(status, headers)
    }

  override def onHttpResponseBodyChunk(chunk: ByteBuf, last: Boolean): Unit =
    if (!done) {
      responseBuilder.accumulate(chunk)
      if (last) {
        done = true
        try {
          responseProcessor.onComplete(responseBuilder.buildResponse)
        } finally {
          responseBuilder.releaseChunks()
        }
      }
    }

  override def onThrowable(throwable: Throwable): Unit = {
    responseBuilder.updateEndTimestamp()
    logger.info(s"Request '${tx.request.requestName}' failed for user ${tx.session.userId}", throwable)
    try {
      responseProcessor.onComplete(responseBuilder.buildFailure(throwable))
    } finally {
      responseBuilder.releaseChunks()
    }
  }

  override def onProtocolAwareness(isHttp2: Boolean): Unit =
    responseBuilder.setHttp2(isHttp2)
}
