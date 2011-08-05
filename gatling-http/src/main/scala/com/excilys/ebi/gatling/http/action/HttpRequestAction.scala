package com.excilys.ebi.gatling.http.action

import com.excilys.ebi.gatling.core.action.{ Action, RequestAction }
import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.ahc.CustomAsyncHandler
import com.excilys.ebi.gatling.http.phase.HttpPhase
import com.excilys.ebi.gatling.http.phase.StatusReceived
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder
import com.excilys.ebi.gatling.http.processor.assertion.builder.HttpAssertionBuilder
import com.excilys.ebi.gatling.http.processor.assertion.HttpStatusAssertion
import com.excilys.ebi.gatling.http.processor.capture.builder.HttpCaptureBuilder
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture

import com.ning.http.client.AsyncHttpClient

import scala.collection.mutable.{ HashMap, MultiMap, Set => MSet }

import java.util.Date

object HttpRequestAction {
  val CLIENT: AsyncHttpClient = new AsyncHttpClient
}
class HttpRequestAction(next: Action, request: HttpRequest, givenProcessorBuilders: Option[List[HttpProcessorBuilder]])
    extends RequestAction(next, request, givenProcessorBuilders) {

  val assertions: MultiMap[HttpPhase, HttpAssertion] = new HashMap[HttpPhase, MSet[HttpAssertion]] with MultiMap[HttpPhase, HttpAssertion]
  val captures: MultiMap[HttpPhase, HttpCapture] = new HashMap[HttpPhase, MSet[HttpCapture]] with MultiMap[HttpPhase, HttpCapture]

  givenProcessorBuilders match {
    case Some(list) => {
      for (processorBuilder <- list) {
        processorBuilder match {
          case a: HttpAssertionBuilder =>
            val assertion = a.build
            assertions.addBinding(assertion.getHttpPhase, assertion)
            logger.debug("  -- Adding {} to {} Phase", assertion, assertion.getHttpPhase)
          case c: HttpCaptureBuilder =>
            val capture = c.build
            captures.addBinding(capture.getHttpPhase, capture)
            logger.debug("  -- Adding {} to {} Phase", capture, capture.getHttpPhase)
        }
      }
    }
    case None => {}
  }

  // Adds default assertions (they won't be added if overrided by user)
  assertions.addBinding(new StatusReceived, new HttpStatusAssertion((200 to 210).mkString(":"), None))

  def execute(context: Context) = {
    logger.info("Sending Request")
    HttpRequestAction.CLIENT.executeRequest(request.getRequest(context), new CustomAsyncHandler(context, assertions, captures, next, System.nanoTime, new Date, request))
  }
}
