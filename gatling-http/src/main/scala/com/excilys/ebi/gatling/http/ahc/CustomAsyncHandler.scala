package com.excilys.ebi.gatling.http.ahc

import scala.collection.mutable.{ HashSet, MultiMap }

import com.ning.http.client.AsyncHandler
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.Response
import com.ning.http.client.Response.ResponseBuilder
import com.ning.http.client.HttpResponseStatus
import com.ning.http.client.HttpResponseHeaders
import com.ning.http.client.HttpResponseBodyPart

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.core.context.builder.ContextBuilder.makeContext
import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.phase.HttpPhase
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.phase._
import com.excilys.ebi.gatling.http.request.HttpRequest

import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.Actor.registry.actorFor

class CustomAsyncHandler(context: Context, processors: MultiMap[HttpPhase, HttpProcessor], next: Action, givenProcessors: Option[List[HttpProcessor]],
  executionStartTime: Long, executionStartDate: Date, request: HttpRequest)
  extends AsyncHandler[Response] with Logging {

  private val responseBuilder: ResponseBuilder = new ResponseBuilder()

  var contextBuilder = makeContext fromContext context

  private def processResponse(httpPhase: HttpPhase, placeToSearch: Any): STATE = {
    for (processor <- processors.get(httpPhase).getOrElse(new HashSet)) {
      processor match {
        case c: HttpCapture => {
          val value = c.capture(placeToSearch)
          logger.info("Captured Value: {}", value)
          contextBuilder = contextBuilder setAttribute (c.getAttrKey, value.getOrElse(throw new Exception("Capture string not found")).toString)
        }
        case a: HttpAssertion => {
          logger.info("Asserting")
        }
        case _ =>
      }
    }
    STATE.CONTINUE
    //continue(httpPhase)
  }

  private def continue(httpPhase: HttpPhase): STATE = {
    givenProcessors match {
      case Some(list) => if (processors.get(httpPhase).size == givenProcessors.get.size) STATE.ABORT else STATE.CONTINUE
      case None => STATE.CONTINUE
    }
  }

  def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {
    responseBuilder.accumulate(responseStatus)
    processResponse(new StatusReceived, responseStatus.getStatusCode)
  }

  def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
    responseBuilder.accumulate(headers)
    processResponse(new HeadersReceived, headers.getHeaders) // Ici c'est compliqué...
  }

  def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
    responseBuilder.accumulate(bodyPart)
    STATE.CONTINUE
  }

  def onCompleted(): Response = {
    logger.debug("Response Received")
    val processingStartTime: Long = System.nanoTime()
    processResponse(new CompletePageReceived, responseBuilder.build)

    actorFor(context.getWriteActorUuid).map { a =>
      a ! ActionInfo(context.getUserId, "Request " + request.getName, executionStartDate, TimeUnit.MILLISECONDS.convert(System.nanoTime - executionStartTime, TimeUnit.NANOSECONDS), "OK", "Request Executed Successfully")
    }

    next.execute(contextBuilder setElapsedActionTime (System.nanoTime() - processingStartTime) build)
    null
  }

  def onThrowable(throwable: Throwable) = {
    throwable.printStackTrace
  }

}
