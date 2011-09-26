package com.excilys.ebi.gatling.http.ahc

import scala.collection.mutable.{ MultiMap, HashMap }
import scala.collection.immutable.HashSet
import scala.collection.{ Set => CSet }

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.builder.ContextBuilder._
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.provider.capture.RegExpCaptureProvider
import com.excilys.ebi.gatling.core.provider.capture.XPathCaptureProvider
import com.excilys.ebi.gatling.core.processor.Check._
import com.excilys.ebi.gatling.core.processor.CheckType._
import com.excilys.ebi.gatling.core.provider.ProviderType._
import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider
import com.excilys.ebi.gatling.core.result.message.ResultStatus._
import com.excilys.ebi.gatling.core.result.message.ActionInfo

import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.provider.capture.HttpHeadersCaptureProvider
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.provider.capture.HttpHeadersCaptureProvider
import com.excilys.ebi.gatling.http.provider.capture.HttpStatusCaptureProvider

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

  private val responseBuilder: ResponseBuilder = new ResponseBuilder()

  private var contextBuilder = newContext fromContext context

  private var hasSentLog = false

  private def isPhaseProcessed(httpPhase: HttpPhase): Boolean = {
    // FIXME current cookie handling implementation requires to systematically process CompletePageReceived
    processors.get(httpPhase).isDefined || httpPhase == CompletePageReceived
  }

  private def isContinueAfterPhase(httpPhase: HttpPhase): Boolean = {
    httpPhase match {
      case StatusReceived =>
        isPhaseProcessed(HeadersReceived) || isPhaseProcessed(CompletePageReceived)
      case HeadersReceived =>
        isPhaseProcessed(CompletePageReceived)
      case CompletePageReceived =>
        false
      case _ =>
        throw new IllegalArgumentException("Phase not supported " + httpPhase)
    }
  }

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

  private def prepareProviders(processors: CSet[HttpProcessor], placeToSearch: Any): HashMap[ProviderType, AbstractCaptureProvider] = {

    val providers: HashMap[ProviderType, AbstractCaptureProvider] = HashMap.empty
    processors.foreach { processor =>

      processor.getProviderType match {
        case REGEXP_PROVIDER =>
          logger.debug("Prepared REGEXP_PROVIDER")
          providers += REGEXP_PROVIDER -> new RegExpCaptureProvider(placeToSearch.asInstanceOf[Response].getResponseBody)

        case XPATH_PROVIDER =>
          logger.debug("Prepared XPATH_PROVIDER")
          providers += XPATH_PROVIDER -> new XPathCaptureProvider(placeToSearch.asInstanceOf[Response].getResponseBodyAsBytes)

        case HTTP_HEADERS_PROVIDER =>
          logger.debug("Prepared HTTP_HEADER_PROVIDER")
          providers += HTTP_HEADERS_PROVIDER -> new HttpHeadersCaptureProvider(placeToSearch.asInstanceOf[FluentCaseInsensitiveStringsMap])

        case HTTP_STATUS_PROVIDER =>
          logger.debug("Prepared HTTP_STATUS_PROVIDER")
          providers += HTTP_STATUS_PROVIDER -> new HttpStatusCaptureProvider(placeToSearch.asInstanceOf[Int])
      }
    }

    providers
  }

  private def captureData(processor: HttpCapture, providers: HashMap[ProviderType, AbstractCaptureProvider]): Option[Any] = {

    val provider = providers.get(processor.getProviderType).getOrElse {
      throw new IllegalArgumentException;
    };

    provider.capture(processor.expressionFormatter.apply(context))
  }

  private def processResponse(httpPhase: HttpPhase, placeToSearch: Any): STATE = {

    if (isPhaseProcessed(httpPhase)) {
      val processingStartTime = System.nanoTime
      logger.debug("Processors at {} : {}", httpPhase, processors.get(httpPhase))

      val phaseProcessors = processors.get(httpPhase).getOrElse(new HashSet[HttpProcessor])

      val response =
        if (placeToSearch.isInstanceOf[Response])
          Some(placeToSearch.asInstanceOf[Response])
        else
          None

      val providers = prepareProviders(phaseProcessors, placeToSearch)

      for (processor <- phaseProcessors) {
        processor match {

          // If the processor is an HTTP Capture
          case c: HttpCapture =>
            val value = captureData(c, providers)
            logger.debug("Captured Value: {}", value)

            if (value.isEmpty && (!c.isInstanceOf[HttpCheck] || c.asInstanceOf[HttpCheck].getCheckType != INEXISTENCE)) {
              sendLogAndExecuteNext(KO, "Capture " + c + " failed", processingStartTime, response)
            } else {
              var contextAttribute = (StringUtils.EMPTY, StringUtils.EMPTY)
              if (!value.isEmpty) {
                contextAttribute = (c.getAttrKey, value.get.toString)
              }

              if (c.isInstanceOf[HttpCheck]) {
                // If the Capture is also a check
                val check = c.asInstanceOf[HttpCheck]
                // Computing of the result of the check
                val result =
                  check.getCheckType match {
                    case EQUALITY => checkEquals(value.get, check.getExpected)
                    case INEQUALITY => !checkEquals(value.get, check.getExpected)
                    case IN_RANGE => checkInRange(value.get, check.getExpected)
                    case EXISTENCE => true
                    case INEXISTENCE => value.isEmpty
                  }

                // If the result is true, then we store the value in the context if requested
                if (result) {
                  logger.debug("CHECK RESULT: true")
                  if (c.getAttrKey != StringUtils.EMPTY)
                    contextBuilder = contextBuilder setAttribute contextAttribute
                } else {
                  logger.warn("CHECK RESULT: false expected {} but received {}", check, value.get.toString)
                  // Else, we write the failure in the logs
                  sendLogAndExecuteNext(KO, "Check " + check + " failed", processingStartTime, response)
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

      // FIXME current cookie handling implementation requires to systematically process CompletePageReceived
      if (httpPhase == CompletePageReceived) {
        sendLogAndExecuteNext(OK, "Request Executed Successfully", processingStartTime, response)
      }
    }

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