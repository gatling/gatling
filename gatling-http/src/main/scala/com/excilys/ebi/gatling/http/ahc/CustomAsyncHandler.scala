package com.excilys.ebi.gatling.http.ahc

import scala.collection.mutable.MultiMap
import scala.collection.immutable.HashSet

import com.excilys.ebi.gatling.http.phase._
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion

import com.excilys.ebi.gatling.core.processor.Assertion._
import com.excilys.ebi.gatling.core.processor.AssertionType._
import com.excilys.ebi.gatling.core.result.message.ResultStatus._
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.builder.ContextBuilder._
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.result.message.ActionInfo

import com.excilys.ebi.gatling.http.processor.HttpProcessor

import akka.actor.Actor.registry._

import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.Response.ResponseBuilder
import com.ning.http.client.AsyncHandler
import com.ning.http.client.HttpResponseBodyPart
import com.ning.http.client.HttpResponseHeaders
import com.ning.http.client.HttpResponseStatus
import com.ning.http.client.Response

import java.util.Date
import java.util.concurrent.TimeUnit

class CustomAsyncHandler(context: Context, processors: MultiMap[HttpPhase, HttpProcessor], next: Action, executionStartTime: Long, executionStartDate: Date, requestName: String)
    extends AsyncHandler[Response] with Logging {

  private val responseBuilder: ResponseBuilder = new ResponseBuilder()

  private var contextBuilder = newContext fromContext context

  private var hasSentLog = false

  private def sendLogAndExecuteNext(requestResult: ResultStatus, requestMessage: String, processingStartTime: Long, response: Option[Response]) = {
    if (!hasSentLog) {
      actorFor(context.getWriteActorUuid).map { a =>
        a ! ActionInfo(context.getScenarioName, context.getUserId, "Request " + requestName, executionStartDate, TimeUnit.MILLISECONDS.convert(System.nanoTime - executionStartTime, TimeUnit.NANOSECONDS), requestResult, requestMessage)
      }
      response.map { r =>
        contextBuilder = contextBuilder setCookies r.getCookies
      }
      next.execute(contextBuilder setDuration (System.nanoTime() - processingStartTime) build)
      hasSentLog = true
    }
  }

  private def processResponse(httpPhase: HttpPhase, placeToSearch: Any): STATE = {
    val processingStartTime = System.nanoTime
    logger.debug("Processors at {} : {}", httpPhase, processors.get(httpPhase))
    for (processor <- processors.get(httpPhase).getOrElse(new HashSet)) {
      processor match {
        // If the processor is an HTTP Capture
        case c: HttpCapture =>
          val value = c.capture(placeToSearch)
          logger.info("Captured Value: {}", value)
          contextBuilder = contextBuilder setAttribute (c.getAttrKey, value.getOrElse(throw new Exception("Capture string not found")).toString)
          if (c.isInstanceOf[HttpAssertion]) {
            // If the Capture is also an assertion
            val a = c.asInstanceOf[HttpAssertion]
            // Computing of the result of the assertion
            val result =
              a.getAssertionType match {
                case EQUALITY => assertEquals(value.get, a.getExpected)
                case IN_RANGE => assertInRange(value.get, a.getExpected)
              }
            logger.debug("ASSERTION RESULT: {}", result)

            // If the result is true, then we store the value in the context if requested
            if (result) {
              contextBuilder = contextBuilder setAttribute (c.getAttrKey, value.getOrElse(throw new Exception("Assertion didn't find result")).toString)
            } else {
              // Else, we write the failure in the logs
              val response =
                if (placeToSearch.isInstanceOf[Response])
                  Some(placeToSearch.asInstanceOf[Response])
                else
                  None
              sendLogAndExecuteNext(KO, "Assertion " + a + " failed", processingStartTime, response)
              return STATE.ABORT
            }
          }
        case _ => throw new IllegalArgumentException
      }
    }

    if (placeToSearch.isInstanceOf[Response])
      sendLogAndExecuteNext(OK, "Request Executed Successfully", processingStartTime, Some(placeToSearch.asInstanceOf[Response]))

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
    sendLogAndExecuteNext(KO, throwable.getMessage, System.nanoTime(), None)
  }

}