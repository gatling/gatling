package com.excilys.ebi.gatling.http.action

import com.excilys.ebi.gatling.core.action.{Action, RequestAction}
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.http.ahc.CustomAsyncHandler
import com.excilys.ebi.gatling.http.context.HttpContext
import com.excilys.ebi.gatling.http.phase.HttpResponseHook
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.ning.http.client.AsyncHttpClient
import scala.collection.mutable.{HashMap, MultiMap, Set => MSet}

object HttpRequestAction {

  val CLIENT: AsyncHttpClient = new AsyncHttpClient
}
class HttpRequestAction(next: Action, request: HttpRequest, givenProcessors: Option[List[HttpProcessor]])
  extends RequestAction(next, request, givenProcessors) with Logging {

  val processors: MultiMap[HttpResponseHook, HttpProcessor] = new HashMap[HttpResponseHook, MSet[HttpProcessor]] with MultiMap[HttpResponseHook, HttpProcessor]

  {
    givenProcessors match {
      case Some(list) => {
        for (processor <- list) {
          logger.debug("Adding {} to {} Phase", processor, processor.getHttpHook)
          processors.addBinding(processor.getHttpHook, processor)
        }
      }
      case None => {}
    }
  }

  def execute(context: Context) = {
    logger.info("Sending Request")
    HttpRequestAction.CLIENT.executeRequest(request.getRequest, new CustomAsyncHandler(context.asInstanceOf[HttpContext], processors, next, givenProcessors))
  }
}
