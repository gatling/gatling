package com.excilys.ebi.gatling.http.ahc

import scala.collection.mutable.MultiMap

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

import com.excilys.ebi.gatling.http.context.HttpContext
import com.excilys.ebi.gatling.http.context.builder.HttpContextBuilder._
import com.excilys.ebi.gatling.http.phase.HttpPhase
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.phase._
import com.excilys.ebi.gatling.http.request.HttpRequest

import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.Actor.registry.actorFor

class CustomAsyncHandler(context: HttpContext, processors: MultiMap[HttpPhase, HttpProcessor], next: Action, givenProcessors: Option[List[HttpProcessor]],
  executionStartTime: Long, executionStartDate: Date, request: HttpRequest)
  extends AsyncHandler[Response] with Logging {

  private val responseBuilder: ResponseBuilder = new ResponseBuilder()

  var contextBuilder = httpContext fromContext context

  private def processResponse(httpPhase: HttpPhase, placeToSearch: Any): STATE = {
    processors.get(httpPhase) match {
      case Some(set) => for (processor <- set) {
        processor match {
          case c: HttpCapture => {
            val value = c.capture(placeToSearch)
            logger.info("Captured Value: {}", value)
            contextBuilder = c.getScope.setAttribute(contextBuilder, c.getAttrKey, value)
          }
          case a: HttpAssertion => {
            logger.info("Asserting")
          }
          case _ =>
        }
      }
      case None =>
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
    actorFor(context.getWriteActorUuid) match {
      case Some(a) =>
        a ! ActionInfo(context.getUserId, "Request " + request.getName, executionStartDate, TimeUnit.MILLISECONDS.convert(System.nanoTime - executionStartTime, TimeUnit.NANOSECONDS), "OK")
      case None =>
    }
    next.execute(contextBuilder setElapsedActionTime (System.nanoTime() - processingStartTime) build)
    null
  }

  def onThrowable(throwable: Throwable) = {
    throwable.printStackTrace
  }

}
