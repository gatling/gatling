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

import java.util.Date
import java.util.concurrent.TimeUnit

import akka.actor.Actor.registry.actorFor

class CustomAsyncHandler(context: Context, assertions: MultiMap[HttpPhase, HttpAssertion], captures: MultiMap[HttpPhase, HttpCapture], next: Action, executionStartTime: Long, executionStartDate: Date,
                         requestName: String)
    extends AsyncHandler[Response] with Logging {

  private val responseBuilder: ResponseBuilder = new ResponseBuilder()

  private var contextBuilder = makeContext fromContext context

  private var hasSentLog = false

  private def sendLogAndExecuteNext(requestResult: String, requestMessage: String, processingStartTime: Long, response: Option[Response]) = {
    if (!hasSentLog) {
      actorFor(context.getWriteActorUuid).map { a =>
        a ! ActionInfo(context.getScenarioName, context.getUserId, "Request " + requestName, executionStartDate, TimeUnit.MILLISECONDS.convert(System.nanoTime - executionStartTime, TimeUnit.NANOSECONDS), requestResult, requestMessage)
      }
      response.map { r =>
        contextBuilder = contextBuilder setCookies r.getCookies
      }
      next.execute(contextBuilder setElapsedActionTime (System.nanoTime() - processingStartTime) build)
      hasSentLog = true
    }
  }

  private def processResponse(httpPhase: HttpPhase, placeToSearch: Any): STATE = {
    val processingStartTime = System.nanoTime
    logger.debug("Assertions at {} : {}", httpPhase, assertions.get(httpPhase))
    for (a <- assertions.get(httpPhase).getOrElse(new HashSet)) {
      val (result, resultValue, attrKey) = a.assertInRequest(placeToSearch, requestName + context.getUserId + executionStartDate)
      logger.debug("ASSERTION RESULT: {}, {}, " + attrKey, result, resultValue)
      if (result)
        attrKey.map { key =>
          contextBuilder = contextBuilder setAttribute (key, resultValue.getOrElse(throw new Exception("Assertion didn't find result")).toString)
        }
      else {
        val response =
          if (placeToSearch.isInstanceOf[Response])
            Some(placeToSearch.asInstanceOf[Response])
          else
            None
        sendLogAndExecuteNext("KO", "Assertion " + a + " failed", processingStartTime, response)
        return STATE.ABORT
      }
    }

    for (c <- captures.get(httpPhase).getOrElse(new HashSet)) {
      val value = c.capture(placeToSearch)
      logger.info("Captured Value: {}", value)
      contextBuilder = contextBuilder setAttribute (c.getAttrKey, value.getOrElse(throw new Exception("Capture string not found")).toString)
    }

    if (placeToSearch.isInstanceOf[Response])
      sendLogAndExecuteNext("OK", "Request Executed Successfully", processingStartTime, Some(placeToSearch.asInstanceOf[Response]))

    STATE.CONTINUE
  }

  def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {
    responseBuilder.accumulate(responseStatus)
    processResponse(new StatusReceived, responseStatus.getStatusCode)
  }

  def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
    responseBuilder.accumulate(headers)
    processResponse(new HeadersReceived, headers.getHeaders)
  }

  def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
    responseBuilder.accumulate(bodyPart)
    STATE.CONTINUE
  }

  def onCompleted(): Response = {
    val response = responseBuilder.build
    processResponse(new CompletePageReceived, response)
    null
  }

  def onThrowable(throwable: Throwable) = {
    logger.debug("{}\n{}", throwable.getClass, throwable.getStackTraceString)
    sendLogAndExecuteNext("KO", throwable.getMessage, System.nanoTime(), None)
  }

}