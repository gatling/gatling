package com.excilys.ebi.gatling.http.action

import com.excilys.ebi.gatling.core.action.{ Action, RequestAction }
import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.ahc.CustomAsyncHandler
import com.excilys.ebi.gatling.http.phase.HttpPhase
import com.excilys.ebi.gatling.http.phase.StatusReceived
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.processor.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.processor.assertion.HttpStatusAssertion
import com.excilys.ebi.gatling.http.processor.capture.HttpCapture

import com.ning.http.client.AsyncHttpClient

import scala.collection.mutable.{ HashMap, MultiMap, Set => MSet }

import java.util.Date

object HttpRequestAction {
  val CLIENT: AsyncHttpClient = new AsyncHttpClient
}
class HttpRequestAction(next: Action, request: HttpRequest, givenProcessors: Option[List[HttpProcessor]])
    extends RequestAction(next, request, givenProcessors) {

  val assertions: MultiMap[HttpPhase, HttpAssertion] = new HashMap[HttpPhase, MSet[HttpAssertion]] with MultiMap[HttpPhase, HttpAssertion]
  val captures: MultiMap[HttpPhase, HttpCapture] = new HashMap[HttpPhase, MSet[HttpCapture]] with MultiMap[HttpPhase, HttpCapture]

  // Adds default assertions
  assertions.addBinding(new StatusReceived, new HttpStatusAssertion((200 to 210).mkString, None))

  givenProcessors match {
    case Some(list) => {
      for (processor <- list) {
        processor match {
          case a: HttpAssertion =>
            assertions.addBinding(a.getHttpPhase, a)
            logger.debug("  -- Adding {} to {} Phase", a, a.getHttpPhase)
          case c: HttpCapture =>
            captures.addBinding(c.getHttpPhase, c)
            logger.debug("  -- Adding {} to {} Phase", c, c.getHttpPhase)
        }
      }
    }
    case None => {}
  }

  def execute(context: Context) = {
    logger.info("Sending Request")
    HttpRequestAction.CLIENT.executeRequest(request.getRequest(context), new CustomAsyncHandler(context, assertions, captures, next, System.nanoTime, new Date, request))
  }
}
