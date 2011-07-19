package com.excilys.ebi.gatling.http.action

import com.excilys.ebi.gatling.core.action.{ Action, RequestAction }
import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.ahc.CustomAsyncHandler
import com.excilys.ebi.gatling.http.phase.HttpPhase
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.request.HttpRequest

import com.ning.http.client.AsyncHttpClient

import scala.collection.mutable.{ HashMap, MultiMap, Set => MSet }

import java.util.Date

object HttpRequestAction {
  val CLIENT: AsyncHttpClient = new AsyncHttpClient
}
class HttpRequestAction(next: Action, request: HttpRequest, givenProcessors: Option[List[HttpProcessor]])
  extends RequestAction(next, request, givenProcessors) {

  val processors: MultiMap[HttpPhase, HttpProcessor] = new HashMap[HttpPhase, MSet[HttpProcessor]] with MultiMap[HttpPhase, HttpProcessor]

  givenProcessors match {
    case Some(list) => {
      for (processor <- list) {
        logger.debug("  -- Adding {} to {} Phase", processor, processor.getHttpPhase)
        processors.addBinding(processor.getHttpPhase, processor)
      }
    }
    case None => {}
  }

  def execute(context: Context) = {
    logger.info("Sending Request")
    HttpRequestAction.CLIENT.executeRequest(request.getRequest(context), new CustomAsyncHandler(context, processors, next, givenProcessors, System.nanoTime, new Date, request))
  }
}
