/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

import com.ning.http.client.providers.netty.request.NettyRequest
import com.ning.http.client.{ AsyncHandlerExtensions, HttpResponseBodyPart, HttpResponseHeaders, HttpResponseStatus, ProgressAsyncHandler }
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.AsyncHandler.STATE.CONTINUE
import com.typesafe.scalalogging.StrictLogging

import scala.util.control.NonFatal

/**
 * This class is the AsyncHandler that AsyncHttpClient needs to process a request's response
 *
 * It is part of the HttpRequestAction
 *
 * @constructor constructs a GatlingAsyncHandler
 * @param tx the data about the request to be sent and processed
 * @param httpEngine the HTTP engine
 */
class AsyncHandler(tx: HttpTx, httpEngine: HttpEngine) extends ProgressAsyncHandler[Unit] with AsyncHandlerExtensions with StrictLogging {

  val responseBuilder = tx.responseBuilderFactory(tx.request.ahcRequest)
  private val init = new AtomicBoolean
  private val done = new AtomicBoolean

  private def start(): Unit =
    if (init.compareAndSet(false, true)) {
      httpEngine.dataWriters.logRequest(tx.session, tx.request.requestName)
      responseBuilder.updateFirstByteSent()
    }

  override def onOpenConnection(): Unit = start()

  override def onConnectionOpen(): Unit = {}

  override def onPoolConnection(): Unit = {}

  override def onConnectionPooled(): Unit = {}

  override def onDnsResolved(address: InetAddress): Unit =
    responseBuilder.setRemoteAddress(address)

  override def onSslHandshakeCompleted(): Unit = {}

  override def onSendRequest(request: Any): Unit = {
    start()
    if (logger.underlying.isDebugEnabled)
      responseBuilder.setNettyRequest(request.asInstanceOf[NettyRequest])
  }

  override def onRetry(): Unit =
    if (!done.get) responseBuilder.reset()
    else logger.error("onRetry is not supposed to be called once done, please report")

  override def onHeaderWriteCompleted: STATE = {
    if (!done.get) responseBuilder.updateLastByteSent()
    CONTINUE
  }

  override def onContentWriteCompleted: STATE = {
    if (!done.get) responseBuilder.updateLastByteSent()
    CONTINUE
  }

  override def onContentWriteProgress(amount: Long, current: Long, total: Long) = CONTINUE

  override def onStatusReceived(status: HttpResponseStatus): STATE = {
    if (!done.get) responseBuilder.accumulate(status)
    CONTINUE
  }

  override def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
    if (!done.get) responseBuilder.accumulate(headers)
    CONTINUE
  }

  override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
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
      responseBuilder.updateLastByteReceived()
      sendOnThrowable(throwable)
    }

  def sendOnThrowable(throwable: Throwable): Unit = {
    val className = throwable.getClass.getName
    val errorMessage = throwable.getMessage match {
      case null => className
      case m    => s"$className: $m"
    }

    if (logger.underlying.isDebugEnabled)
      logger.debug(s"Request '${tx.request.requestName}' failed for user ${tx.session.userId}", throwable)
    else
      logger.info(s"Request '${tx.request.requestName}' failed for user ${tx.session.userId}: $errorMessage")

    httpEngine.asyncHandlerActors ! OnThrowable(tx, responseBuilder.build, errorMessage)
  }
}
