package com.excilys.ebi.gatling.http.action

import com.excilys.ebi.gatling.core.action.{ Action, RequestAction }
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.http.ahc.CustomAsyncHandler
import com.excilys.ebi.gatling.http.request.HttpPhase._
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.processor.check.HttpStatusCheck
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.ning.http.client.AsyncHttpClient
import scala.collection.mutable.{ HashMap, MultiMap, Set => MSet }
import java.util.Date
import com.ning.http.client.AsyncHttpClientConfig
import org.apache.commons.lang3.StringUtils

object HttpRequestAction {
  val CLIENT: AsyncHttpClient = new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setCompressionEnabled(true).build())
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

  // Adds default checks (they won't be added if overridden by user)
  processors.addBinding(StatusReceived, new HttpStatusCheck((200 to 210).mkString(":"), StringUtils.EMPTY))

  def execute(context: Context) = {
    val objects = new Array[java.lang.Object](3)
    objects(0) = request.name
    objects(1) = context.getScenarioName
    objects(2) = context.getUserId.toString
    logger.info("Sending Request '{}': Scenario '{}', UserId #{}", objects)
    HttpRequestAction.CLIENT.executeRequest(request.getRequest(context), new CustomAsyncHandler(context, processors, next, System.nanoTime, new Date, request.getName))
  }
}
