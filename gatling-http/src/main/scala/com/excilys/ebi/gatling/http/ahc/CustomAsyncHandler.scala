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

import com.excilys.ebi.gatling.http.context.HttpContext
import com.excilys.ebi.gatling.http.context.builder.HttpContextBuilder._
import com.excilys.ebi.gatling.http.phase.HttpResponseHook
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.capture.HttpCapture
import com.excilys.ebi.gatling.http.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.phase._

class CustomAsyncHandler(context: HttpContext, processors: MultiMap[HttpResponseHook, HttpProcessor], next: Action, givenProcessors: Option[List[HttpProcessor]])
  extends AsyncHandler[Response] with Logging {

  private val responseBuilder: ResponseBuilder = new ResponseBuilder()

  var contextBuilder = httpContext fromContext context

  private def processResponse(httpHook: HttpResponseHook, placeToSearch: Any): STATE = {
    processors.get(httpHook) match {
      case Some(set) => for (processor <- set) {
        if (processor.isInstanceOf[HttpCapture]) {
          val c = processor.asInstanceOf[HttpCapture]
          val value = c.capture(placeToSearch)
          logger.info("Captured Value: {}", value)
          contextBuilder = c.getScope.setAttribute(contextBuilder, c.getAttrKey, value)
        } else if (processor.isInstanceOf[HttpAssertion]) {
          val a = processor.asInstanceOf[HttpAssertion]
        }
      }
      case None =>
    }
    STATE.CONTINUE
    //continue(httpHook)
  }

  private def continue(httpHook: HttpResponseHook): STATE = {
    givenProcessors match {
      case Some(list) => if (processors.get(httpHook).size == givenProcessors.get.size) STATE.ABORT else STATE.CONTINUE
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
    val startTime: Long = System.nanoTime()
    processResponse(new CompletePageReceived, responseBuilder.build.getResponseBody)
    next.execute(contextBuilder setElapsedActionTime (System.nanoTime() - startTime) build)
    null
  }

  def onThrowable(throwable: Throwable) = {
    throwable.printStackTrace
  }

}
