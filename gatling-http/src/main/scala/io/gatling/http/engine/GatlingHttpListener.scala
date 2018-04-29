/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import scala.util.control.NonFatal

import io.gatling.commons.util.Clock
import io.gatling.commons.util.Throwables._
import io.gatling.http.action.HttpTx
import io.gatling.http.client.HttpListener
import io.gatling.http.response
import io.gatling.http.response.Response

import com.typesafe.scalalogging._
import io.netty.handler.codec.http.{ HttpHeaders, HttpResponseStatus }
import io.netty.buffer.ByteBuf

object GatlingHttpListener extends StrictLogging {
  private val DebugEnabled = logger.underlying.isDebugEnabled
  private val InfoEnabled = logger.underlying.isInfoEnabled
}

/**
 * This class is the AsyncHandler that AsyncHttpClient needs to process a request's response
 *
 * It is part of the HttpRequestAction
 *
 * @constructor constructs a Gatling AsyncHandler
 * @param tx the data about the request to be sent and processed
 * @param responseProcessor the responseProcessor
 */
class GatlingHttpListener(tx: HttpTx, responseProcessor: ResponseProcessor, clock: Clock) extends HttpListener with LazyLogging {

  private val responseBuilder = tx.responseBuilderFactory(tx.request.clientRequest)
  private var init = false
  private var done = false
  // [fl]
  //
  //
  //
  //
  // [fl]

  private[http] def start(): Unit =
    if (!init) {
      init = true
      responseBuilder.updateStartTimestamp()
      // [fl]
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
  // [fl]

  override def onHttpResponse(status: HttpResponseStatus, headers: HttpHeaders): Unit =
    if (!done) {
      responseBuilder.accumulate(status, headers)
    }

  override def onHttpResponseBodyChunk(chunk: ByteBuf, last: Boolean): Unit =
    if (!done) {
      responseBuilder.accumulate(chunk)
      if (last) {
        withResponse { response =>
          try {
            responseProcessor.onCompleted(tx, response)
          } catch {
            case NonFatal(t) => sendOnThrowable(response, t)
          }
        }
      }
    }

  private def withResponse(f: response.Response => Unit): Unit =
    if (!done) {
      done = true
      try {
        val response = responseBuilder.build
        f(response)
      } catch {
        case NonFatal(t) => sendOnThrowable(responseBuilder.buildSafeResponse, t)
      }
    }

  override def onThrowable(throwable: Throwable): Unit =
    withResponse { response =>
      responseBuilder.updateEndTimestamp()
      sendOnThrowable(response, throwable)
    }

  private def sendOnThrowable(response: Response, throwable: Throwable): Unit = {
    val errorMessage = throwable.detailedMessage

    if (GatlingHttpListener.DebugEnabled)
      logger.debug(s"Request '${tx.request.requestName}' failed for user ${tx.session.userId}", throwable)
    else if (GatlingHttpListener.InfoEnabled)
      logger.info(s"Request '${tx.request.requestName}' failed for user ${tx.session.userId}: $errorMessage")

    responseProcessor.onThrowable(tx, response, errorMessage)
  }
}
