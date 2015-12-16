/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

import scala.util.control.NonFatal

import io.gatling.commons.util.ClassHelper._
import io.gatling.commons.util.TimeHelper.nowMillis
import io.gatling.http.action.sync.HttpTx

import org.asynchttpclient._
import org.asynchttpclient.AsyncHandler.State
import org.asynchttpclient.AsyncHandler.State._
import org.asynchttpclient.handler._
import org.asynchttpclient.netty.request.NettyRequest
import com.typesafe.scalalogging._
import io.netty.channel.Channel

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
 * @param httpEngine the HTTP engine
 */
class AsyncHandler(tx: HttpTx, httpEngine: HttpEngine) extends ExtendedAsyncHandler[Unit] with ProgressAsyncHandler[Unit] with LazyLogging {

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

  private def start(): Unit =
    if (init.compareAndSet(false, true)) {
      val firstByteSent = responseBuilder.updateStartTimestamp()
      // [fl]
      //
      // [fl]
    }

  override def onHostnameResolutionAttempt(name: String): Unit = {
    start()
    // [fl]
    //
    // [fl]
  }

  override def onTcpConnectAttempt(remoteAddress: InetSocketAddress): Unit = {
    start()
    // [fl]
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
  // [fl]

  override def onRequestSend(request: NettyRequest): Unit = {
    start()
    if (AsyncHandler.DebugEnabled)
      responseBuilder.setNettyRequest(request.asInstanceOf[NettyRequest])
  }

  override def onRetry(): Unit =
    if (!done.get) responseBuilder.reset()
    else logger.error("onRetry is not supposed to be called once done, please report")

  override val onHeadersWritten: State = CONTINUE

  override val onContentWritten: State = CONTINUE

  override def onContentWriteProgress(amount: Long, current: Long, total: Long) = CONTINUE

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

  override def onCompleted: Unit =
    if (done.compareAndSet(false, true)) {
      try { httpEngine.asyncHandlerActors ! OnCompleted(tx, responseBuilder.build) }
      catch { case NonFatal(e) => sendOnThrowable(e) }
    }

  override def onThrowable(throwable: Throwable): Unit =
    if (done.compareAndSet(false, true)) {
      responseBuilder.updateEndTimestamp()
      sendOnThrowable(throwable)
    }

  def sendOnThrowable(throwable: Throwable): Unit = {
    val classShortName = throwable.getClass.getShortName
    val errorMessage = throwable.getMessage match {
      case null => classShortName
      case m    => s"$classShortName: $m"
    }

    if (AsyncHandler.DebugEnabled)
      logger.debug(s"Request '${tx.request.requestName}' failed for user ${tx.session.userId}", throwable)
    else if (AsyncHandler.InfoEnabled)
      logger.info(s"Request '${tx.request.requestName}' failed for user ${tx.session.userId}: $errorMessage")

    httpEngine.asyncHandlerActors ! OnThrowable(tx, responseBuilder.build, errorMessage)
  }
}
