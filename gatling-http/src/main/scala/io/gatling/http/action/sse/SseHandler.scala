package io.gatling.http.action.sse

import java.util.concurrent.atomic.AtomicBoolean
import javax.xml.ws.http.HTTPException

import akka.actor.ActorRef
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.AsyncHandler.STATE.CONTINUE
import com.ning.http.client._
import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.http.ahc.SseTx

/**
 * @author ctranxuan
 */
class SseHandler(tx: SseTx, sseActor: ActorRef) extends AsyncHandler[Unit]
    with AsyncHandlerExtensions
    with SseForwarder
    with EventStreamDispatcher
    with StrictLogging {

  private val sseParser = new EventStreamParser(this)
  private val done = new AtomicBoolean
  private var state: SseState = Opening

  override def onOpenConnection(): Unit = {
  }

  override def onPoolConnection(): Unit = {}

  override def onConnectionPooled(): Unit = {}

  override def onConnectionOpen(): Unit = {
    state = Open
  }

  override def onRetry(): Unit = {
    if (done.get)
      logger.error("onRetry is not supposed to be called once done")
  }

  override def onSendRequest(request: scala.Any): Unit = {
    logger.debug(s"Request ${request} has been sent by the http client")
    sseActor ! OnSend(tx)
  }

  override def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {
    logger.debug(s"Status ${responseStatus.getStatusCode} received for sse '${tx.requestName}")

    responseStatus.getStatusCode match {
      case 200 =>
        CONTINUE

      case unexpected =>
        onThrowable(new HTTPException(unexpected) {
          override def getMessage: String = s"Server returned http response with code ${responseStatus.getStatusCode}"
        })
        STATE.ABORT
    }
  }

  override def onHeadersReceived(headers: HttpResponseHeaders): STATE = CONTINUE

  override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
    if (!done.get) {
      val message = new String(bodyPart.getBodyPartBytes)
      sseParser.parse(message)
    }
    CONTINUE
  }

  override def onCompleted(): Unit = {
    sseActor ! OnClose
  }

  override def onThrowable(throwable: Throwable): Unit = {
    if (done.compareAndSet(false, true)) {
      sendOnThrowable(throwable)
    }
  }

  def sendOnThrowable(throwable: Throwable): Unit = {
    val className = throwable.getClass.getName
    val errorMessage = throwable.getMessage match {
      case null => className
      case m    => s"$className: $m"
    }

    if (logger.underlying.isDebugEnabled)
      logger.debug(s"Request '${tx.requestName}' failed for user ${tx.session.userId}", throwable)
    else
      logger.info(s"Request '${tx.requestName}' failed for user ${tx.session.userId}: $errorMessage")

    state match {
      case Opening =>
        sseActor ! OnFailedOpen(tx, errorMessage, nowMillis)

      case Open =>
        sseActor ! OnThrowable(tx, errorMessage, nowMillis)

      case Closed =>
        logger.error(s"unexpected state closed with error message: $errorMessage")
    }

  }

  override def stopForward(): Unit = {
    done.compareAndSet(false, true)
  }

  override def onDnsResolved(): Unit = {

  }

  override def onSslHandshakeCompleted(): Unit = {

  }

  override def dispatchEventStream(sse: ServerSentEvent): Unit = {
    sseActor ! OnMessage(sse.asJSONString(), nowMillis, this)
  }
}

private sealed trait SseState

private case object Opening extends SseState

private case object Open extends SseState

private case object Closed extends SseState
