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
package io.gatling.http.ahc

import java.util.concurrent.atomic.AtomicBoolean

import scala.util.control.NonFatal

import io.gatling.commons.util.ClassHelper._
import io.gatling.http.action.sync.HttpTx
import io.gatling.http.response.Response

import org.asynchttpclient.{ Response => _, _ }
import org.asynchttpclient.AsyncHandler.State
import org.asynchttpclient.AsyncHandler.State._
import org.asynchttpclient.handler._
import org.asynchttpclient.netty.request.NettyRequest
import com.typesafe.scalalogging._

object AsyncHandler extends StrictLogging {
  val DebugEnabled = logger.underlying.isDebugEnabled
  val InfoEnabled = logger.underlying.isInfoEnabled
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
class AsyncHandler(tx: HttpTx, responseProcessor: ResponseProcessor) extends ExtendedAsyncHandler[Unit] with LazyLogging {

  val responseBuilder = tx.responseBuilderFactory(tx.request.ahcRequest)
  private val init = new AtomicBoolean
  private val done = new AtomicBoolean
  // [fl]
  //
  //
  //
  //
  //
  // [fl]

  private[http] def start(): Unit =
    if (init.compareAndSet(false, true)) {
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
  //
  //
  //
  //
  // [fl]

  override def onRequestSend(request: NettyRequest): Unit = {
    responseBuilder.doReset()
    if (AsyncHandler.DebugEnabled) {
      responseBuilder.setNettyRequest(request.asInstanceOf[NettyRequest])
    }
  }

  override def onRetry(): Unit =
    if (!done.get) responseBuilder.markReset()

  override def onStatusReceived(status: HttpResponseStatus): State = {
    if (!done.get) responseBuilder.accumulate(status)
    CONTINUE
  }

  override def onHeadersReceived(headers: HttpResponseHeaders): State = {
    if (!done.get) responseBuilder.accumulate(headers)
    CONTINUE
  }

  override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): State = {
    if (!done.get) responseBuilder.accumulate(bodyPart)
    CONTINUE
  }

  private def withResponse(f: Response => Unit): Unit =
    if (done.compareAndSet(false, true)) {
      try {
        val response = responseBuilder.build
        f(response)
      } catch {
        case NonFatal(t) => sendOnThrowable(responseBuilder.buildSafeResponse, t)
      }
    }

  override def onCompleted: Unit =
    withResponse { response =>
      try {
        responseProcessor.onCompleted(tx, response)
      } catch {
        case NonFatal(t) => sendOnThrowable(response, t)
      }
    }

  override def onThrowable(throwable: Throwable): Unit =
    withResponse { response =>
      responseBuilder.updateEndTimestamp()
      sendOnThrowable(response, throwable)
    }

  private def sendOnThrowable(response: Response, throwable: Throwable): Unit = {
    val classShortName = throwable.getClass.getShortName
    val errorMessage = throwable.getMessage match {
      case null => classShortName
      case m    => s"$classShortName: $m"
    }

    if (AsyncHandler.DebugEnabled)
      logger.debug(s"Request '${tx.request.requestName}' failed for user ${tx.session.userId}", throwable)
    else if (AsyncHandler.InfoEnabled)
      logger.info(s"Request '${tx.request.requestName}' failed for user ${tx.session.userId}: $errorMessage")

    responseProcessor.onThrowable(tx, response, errorMessage)
  }
}
