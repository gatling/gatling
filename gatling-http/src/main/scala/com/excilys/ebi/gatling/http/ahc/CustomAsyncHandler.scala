package com.excilys.ebi.gatling.http.ahc

import scala.collection.mutable.{ MultiMap, HashMap }
import scala.collection.immutable.HashSet
import scala.collection.{ Set => CSet }
import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.builder.ContextBuilder._
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.provider.capture.AbstractCaptureProvider
import com.excilys.ebi.gatling.core.provider.ProviderType
import com.excilys.ebi.gatling.core.result.message.ResultStatus._
import com.excilys.ebi.gatling.core.result.message.ActionInfo
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.check.HttpCheck
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.Response.ResponseBuilder
import com.ning.http.client.AsyncHandler
import com.ning.http.client.HttpResponseBodyPart
import com.ning.http.client.HttpResponseHeaders
import com.ning.http.client.HttpResponseStatus
import com.ning.http.client.Response
import com.ning.http.client.FluentCaseInsensitiveStringsMap
import com.ning.http.client.Cookie
import com.ning.http.util.AsyncHttpProviderUtils._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import akka.actor.Actor.registry._
import org.apache.commons.lang3.StringUtils
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CustomAsyncHandler(context: Context, processors: MultiMap[HttpPhase, HttpProcessor], next: Action, executionStartTimeNano: Long, executionStartDate: Date, requestName: String)
  extends AsyncHandler[Response] with Logging {

  private val identifier = requestName + context.getUserId

  private val responseBuilder: ResponseBuilder = new ResponseBuilder()

  private var contextBuilder = newContext fromContext context

  private var hasSentLog = new AtomicBoolean(false)

  private var processingStartTimeNano = System.nanoTime()

  private def isPhaseToBeProcessed(httpPhase: HttpPhase): Boolean = {
    (processors.get(httpPhase).isDefined && !hasSentLog.get()) || httpPhase == HeadersReceived
  }

  private def isContinueAfterPhase(httpPhase: HttpPhase): Boolean = {
    httpPhase match {
      case StatusReceived =>
        isPhaseToBeProcessed(HeadersReceived) || isPhaseToBeProcessed(CompletePageReceived)
      case HeadersReceived =>
        isPhaseToBeProcessed(CompletePageReceived)
      case CompletePageReceived =>
        false
      case _ =>
        throw new IllegalArgumentException("Phase not supported " + httpPhase)
    }
  }

  private def sendLogAndExecuteNext(requestResult: ResultStatus, requestMessage: String) = {
    if (hasSentLog.compareAndSet(false, true)) {
      actorFor(context.getWriteActorUuid).map { a =>
        val responseTimeMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime - executionStartTimeNano, TimeUnit.NANOSECONDS)
        a ! ActionInfo(context.getScenarioName, context.getUserId, "Request " + requestName, executionStartDate, responseTimeMillis, requestResult, requestMessage)
      }

      val sentContext = contextBuilder setDuration (System.nanoTime() - processingStartTimeNano) build

      logger.debug("Context Cookies sent to next action: {}", sentContext.getCookies)
      next.execute(sentContext)
    }
  }

  private def prepareProviders(processors: CSet[HttpProcessor], placeToSearch: Any): HashMap[ProviderType, AbstractCaptureProvider] = {

    val providers: HashMap[ProviderType, AbstractCaptureProvider] = HashMap.empty
    processors.foreach { processor =>
      val providerType = processor.getProviderType
      if (providers.get(providerType).isEmpty)
        providers += (providerType -> providerType.getProvider(placeToSearch))
    }

    providers
  }

  private def captureData(processor: HttpCapture, providers: HashMap[ProviderType, AbstractCaptureProvider]): Option[String] = {

    val provider = providers.get(processor.getProviderType).getOrElse {
      throw new IllegalArgumentException;
    };

    provider.capture(processor.expressionFormatter.apply(context))
  }

  private def processResponse(httpPhase: HttpPhase, placeToSearch: Any) {

    if (isPhaseToBeProcessed(httpPhase)) {
      logger.debug("Processors at {} : {}", httpPhase, processors.get(httpPhase))

      val phaseProcessors = processors.get(httpPhase).getOrElse(new HashSet[HttpProcessor])

      val providers = prepareProviders(phaseProcessors, placeToSearch)

      for (processor <- phaseProcessors) {
        processor match {

          // If the processor is an HTTP Capture
          case c: HttpCapture =>
            val value = captureData(c, providers)
            logger.debug("Captured Value: {}", value)

            // Is the value what is expected ?
            val isResultValid =
              if (c.isInstanceOf[HttpCheck])
                c.asInstanceOf[HttpCheck].getResult(value)
              else
                value.isDefined

            if (isResultValid) {
              if (c.getAttrKey != StringUtils.EMPTY && value.isDefined)
                contextBuilder = contextBuilder setAttribute (c.getAttrKey, value.get.toString)
            } else {
              if (c.isInstanceOf[HttpCheck])
                logger.warn("CHECK RESULT: false expected {} but received {}", c, value)
              else
                logger.warn("Capture {} could not get value required by user", c)
              sendLogAndExecuteNext(KO, c + " failed")
            }

          case _ => throw new IllegalArgumentException
        }
      }
    }
  }

  def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {

    processingStartTimeNano = System.nanoTime()

    if (isPhaseToBeProcessed(StatusReceived)) {
      responseBuilder.accumulate(responseStatus)
      processResponse(StatusReceived, responseStatus.getStatusCode)
    }
    STATE.CONTINUE
  }

  def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
    if (isPhaseToBeProcessed(HeadersReceived)) {
      responseBuilder.accumulate(headers)
      val headersMap = headers.getHeaders
      var cookiesList = new java.util.ArrayList[Cookie]

      val setCookieHeaders = headersMap.get(SET_COOKIE)
      if (setCookieHeaders != null) {
        val it = headersMap.get(SET_COOKIE).iterator
        while (it.hasNext)
          cookiesList.add(parseCookie(it.next))

        logger.debug("Cookies extracted: {}", cookiesList)
        contextBuilder = contextBuilder setCookies cookiesList
      }

      processResponse(HeadersReceived, headersMap)
    }
    STATE.CONTINUE
  }

  def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
    if (isPhaseToBeProcessed(CompletePageReceived)) {
      responseBuilder.accumulate(bodyPart)
    }
    STATE.CONTINUE
  }

  def onCompleted(): Response = {
    if (isPhaseToBeProcessed(CompletePageReceived)) {
      val response = responseBuilder.build
      processResponse(CompletePageReceived, response)
    }
    sendLogAndExecuteNext(OK, "Request Executed Successfully")
    null
  }

  def onThrowable(throwable: Throwable) = {
    logger.error("{}\n{}", throwable.getClass, throwable.getStackTraceString)
    sendLogAndExecuteNext(KO, throwable.getMessage)
  }

}