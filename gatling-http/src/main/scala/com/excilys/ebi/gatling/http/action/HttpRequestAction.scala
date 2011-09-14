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
import com.excilys.ebi.gatling.http.processor.HttpProcessor

object HttpRequestAction {
  val CLIENT: AsyncHttpClient = new AsyncHttpClient
}
class HttpRequestAction(next: Action, request: HttpRequest, givenProcessorBuilders: Option[List[HttpProcessorBuilder]])
    extends RequestAction(next, request, givenProcessorBuilders) {

  val processors: MultiMap[HttpPhase, HttpProcessor] = new HashMap[HttpPhase, MSet[HttpProcessor]] with MultiMap[HttpPhase, HttpProcessor]

  givenProcessorBuilders match {
    case Some(list) => {
      for (processorBuilder <- list) {
        val processor = processorBuilder.build
        processors.addBinding(processor.getHttpPhase, processor)
        logger.debug("  -- Adding {} to {} Phase", processor, processor.getHttpPhase)
      }
    }
    case None => {}
  }

  // Adds default assertions (they won't be added if overridden by user)
  processors.addBinding(new StatusReceived, new HttpStatusAssertion((200 to 210).mkString(":"), ""))

  def execute(context: Context) = {
    logger.info("Sending Request")
    HttpRequestAction.CLIENT.executeRequest(request.getRequest(context), new CustomAsyncHandler(context, processors, next, System.nanoTime, new Date, request.getName))
  }
}
