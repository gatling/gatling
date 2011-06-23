package com.excilys.ebi.gatling.http.action

import scala.collection.mutable.{ Set => MSet, HashMap, MultiMap }

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.RequestAction
import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.context.HttpContext
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.phase.HttpResponseHook
import com.excilys.ebi.gatling.http.ahc.CustomAsyncHandler

import com.ning.http.client.AsyncHttpClient

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object HttpRequestAction {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[HttpRequestAction]);
}
class HttpRequestAction(next: Action, request: HttpRequest, givenProcessors: Option[List[HttpProcessor]])
  extends RequestAction(next, request, givenProcessors) {
  val client: AsyncHttpClient = new AsyncHttpClient

  val processors: MultiMap[HttpResponseHook, HttpProcessor] = new HashMap[HttpResponseHook, MSet[HttpProcessor]] with MultiMap[HttpResponseHook, HttpProcessor]

  {
    givenProcessors match {
      case Some(list) => {
        for (processor <- list) {
          HttpRequestAction.LOGGER.debug("Adding {} to {} Phase", processor, processor.getHttpHook)
          processors.addBinding(processor.getHttpHook, processor)
        }
      }
      case None => {}
    }
  }

  def execute(context: Context) = {
    HttpRequestAction.LOGGER.info("Sending Request")
    client.executeRequest(request.getRequest, new CustomAsyncHandler(context.asInstanceOf[HttpContext], processors, next, givenProcessors))
  }
}
