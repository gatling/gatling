package com.excilys.ebi.gatling.http.ahc

import scala.collection.mutable.MultiMap
import scala.collection.immutable.HashSet
import scala.collection.{ Set => CSet }

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.builder.ContextBuilder._
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.provider.capture.RegExpCaptureProvider
import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider
import com.excilys.ebi.gatling.core.processor.Assertion._
import com.excilys.ebi.gatling.core.processor.AssertionType._
import com.excilys.ebi.gatling.core.provider.ProviderType._
import com.excilys.ebi.gatling.core.result.message.ResultStatus._
import com.excilys.ebi.gatling.core.result.message.ActionInfo

import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.provider.capture.HttpHeadersCaptureProvider
import com.excilys.ebi.gatling.http.provider.capture.HttpStatusCaptureProvider
import com.excilys.ebi.gatling.http.request.HttpPhase._

import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.Response.ResponseBuilder
import com.ning.http.client.AsyncHandler
import com.ning.http.client.HttpResponseBodyPart
import com.ning.http.client.HttpResponseHeaders
import com.ning.http.client.HttpResponseStatus
import com.ning.http.client.Response
import com.ning.http.client.FluentCaseInsensitiveStringsMap

import akka.actor.Actor.registry._

import org.apache.commons.lang3.StringUtils

import java.util.Date
import java.util.concurrent.TimeUnit

class CustomAsyncHandler(context: Context, processors: MultiMap[HttpPhase, HttpProcessor], next: Action, executionStartTime: Long, executionStartDate: Date, requestName: String)
    extends AsyncHandler[Response] with Logging {

  private val identifier = requestName + context.getUserId

  private var providerTypes: HashSet[ProviderType] = HashSet.empty

  private val responseBuilder: ResponseBuilder = new ResponseBuilder()

  private var contextBuilder = newContext fromContext context

  private var hasSentLog = false

  private var regexpProvider: RegExpCaptureProvider = null

  private var xpathProvider: XPathCaptureProvider = null

  private var httpHeadersProvider: HttpHeadersCaptureProvider = null

  private var httpStatusProvider: HttpStatusCaptureProvider = null

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

  private def prepareProviders(processors: CSet[HttpProcessor], placeToSearch: Any) = {

    for (processor <- processors) {
      providerTypes = providerTypes + processor.getProviderType
    }

    for (providerType <- providerTypes) {
      providerType match {
        case REGEXP_PROVIDER =>
          logger.debug("Prepared REGEXP_PROVIDER")
          regexpProvider = new RegExpCaptureProvider(placeToSearch.asInstanceOf[Response].getResponseBody)
        case XPATH_PROVIDER =>
          logger.debug("Prepared XPATH_PROVIDER")
          xpathProvider = new XPathCaptureProvider(placeToSearch.asInstanceOf[Response].getResponseBodyAsBytes)
        case HTTP_HEADERS_PROVIDER =>
          logger.debug("Prepared HTTP_HEADER_PROVIDER")
          httpHeadersProvider = new HttpHeadersCaptureProvider(placeToSearch.asInstanceOf[FluentCaseInsensitiveStringsMap])
        case HTTP_STATUS_PROVIDER =>
          logger.debug("Prepared HTTP_STATUS_PROVIDER")
          httpStatusProvider = new HttpStatusCaptureProvider(placeToSearch.asInstanceOf[Int])
      }
    }
  }

  private def captureData(processor: HttpCapture): Option[Any] = {
    processor.getProviderType match {
      case REGEXP_PROVIDER =>
        logger.debug("Captured with REGEXP_PROVIDER")
        regexpProvider.capture(processor.expression)
      case XPATH_PROVIDER =>
        logger.debug("Captured with XPATH_PROVIDER")
        xpathProvider.capture(processor.expression)
      case HTTP_HEADERS_PROVIDER =>
        logger.debug("Captured with HTTP_HEADER_PROVIDER")
        httpHeadersProvider.capture(processor.expression)
      case HTTP_STATUS_PROVIDER =>
        logger.debug("Captured with HTTP_STATUS_PROVIDER")
        httpStatusProvider.capture(processor.expression)
    }
  }

  private def processResponse(httpPhase: HttpPhase, placeToSearch: Any): STATE = {
    val processingStartTime = System.nanoTime
    logger.debug("Processors at {} : {}", httpPhase, processors.get(httpPhase))

    val phaseProcessors = processors.get(httpPhase).getOrElse(new HashSet[HttpProcessor])

    val response =
      if (placeToSearch.isInstanceOf[Response])
        Some(placeToSearch.asInstanceOf[Response])
      else
        None

    prepareProviders(phaseProcessors, placeToSearch)

    for (processor <- phaseProcessors) {
      processor match {

        // If the processor is an HTTP Capture
        case c: HttpCapture =>
          val value = captureData(c)
          logger.debug("Captured Value: {}", value)

          if (value.isEmpty) {
            sendLogAndExecuteNext(KO, "Capture " + c + " failed", processingStartTime, response)
          } else {
            val contextAttribute = (c.getAttrKey, value.get.toString)

            if (c.isInstanceOf[HttpAssertion]) {
              // If the Capture is also an assertion
              val a = c.asInstanceOf[HttpAssertion]
              // Computing of the result of the assertion
              val result =
                a.getAssertionType match {
                  case EQUALITY => assertEquals(value.get, a.getExpected)
                  case IN_RANGE => assertInRange(value.get, a.getExpected)
                  case EXISTENCE => true
                }
              logger.debug("ASSERTION RESULT: {}", result)

              // If the result is true, then we store the value in the context if requested
              if (result) {
                if (c.getAttrKey != StringUtils.EMPTY)
                  contextBuilder = contextBuilder setAttribute contextAttribute
              } else {
                // Else, we write the failure in the logs
                sendLogAndExecuteNext(KO, "Assertion " + a + " failed", processingStartTime, response)
                return STATE.ABORT
              }
            } else {
              if (c.getAttrKey != StringUtils.EMPTY)
                contextBuilder = contextBuilder setAttribute contextAttribute
            }
          }

        case _ => throw new IllegalArgumentException
      }
    }

    if (placeToSearch.isInstanceOf[Response])
      sendLogAndExecuteNext(OK, "Request Executed Successfully", processingStartTime, response)

    providerTypes = HashSet.empty

    STATE.CONTINUE
  }

  def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {
    responseBuilder.accumulate(responseStatus)
    processResponse(StatusReceived, responseStatus.getStatusCode)
  }

  def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
    responseBuilder.accumulate(headers)
    processResponse(HeadersReceived, headers.getHeaders)
  }

  def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
    responseBuilder.accumulate(bodyPart)
    STATE.CONTINUE
  }

  def onCompleted(): Response = {
    val response = responseBuilder.build
    processResponse(CompletePageReceived, response)
    null
  }

  def onThrowable(throwable: Throwable) = {
    logger.error("{}\n{}", throwable.getClass, throwable.getStackTraceString)
    sendLogAndExecuteNext(KO, throwable.getMessage, System.nanoTime(), None)
  }

}