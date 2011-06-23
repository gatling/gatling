package com.excilys.ebi.gatling.http.action

import scala.collection.mutable.{ Set => MSet, HashMap, MultiMap }

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.RequestAction
import com.excilys.ebi.gatling.core.action.request.AbstractRequest
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.Processor

import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.assertion.HttpAssertion
import com.excilys.ebi.gatling.http.context.HttpContext
import com.excilys.ebi.gatling.http.context.builder.HttpContextBuilder._
import com.excilys.ebi.gatling.http.capture.HttpCapture
import com.excilys.ebi.gatling.http.processor.HttpProcessor
import com.excilys.ebi.gatling.http.phase._
import com.excilys.ebi.gatling.http.context._

import com.ning.http.client.AsyncHandler
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client.AsyncHttpClient
import com.ning.http.client.HttpResponseStatus
import com.ning.http.client.HttpResponseHeaders
import com.ning.http.client.HttpResponseBodyPart
import com.ning.http.client.HttpContent
import com.ning.http.client.Response
import com.ning.http.client.Response.ResponseBuilder

class HttpRequestAction(next: Action, request: HttpRequest, givenProcessors: Option[List[HttpProcessor]])
  extends RequestAction(next, request, givenProcessors) {
  val client: AsyncHttpClient = new AsyncHttpClient

  val processors: MultiMap[HttpResponseHook, HttpProcessor] = new HashMap[HttpResponseHook, MSet[HttpProcessor]] with MultiMap[HttpResponseHook, HttpProcessor]

  givenProcessors match {
    case Some(list) => {
      for (processor <- list)
        processors.addBinding(processor.getHttpHook, processor)
    }
    case None =>
  }

  def execute(context: Context) = {
    println("Sending request")
    client.executeRequest(request.getRequest, new AsyncHandler[Response]() {
      private val responseBuilder: ResponseBuilder = new ResponseBuilder()

      var contextBuilder = httpContext fromContext context.asInstanceOf[HttpContext]

      private def processResponse(httpHook: HttpResponseHook, placeToSearch: Any): STATE = {
        for (processor <- processors.get(httpHook)) {
          processor match {
            case c: HttpCapture => {
              contextBuilder = c.getScope.setAttribute(contextBuilder, c.getAttrKey, c.capture(placeToSearch))
            }
            case a: HttpAssertion => {
              // do assertion stuff
            }
            case _ =>
          }
        }
        continue(httpHook)
      }

      private def continue(httpHook: HttpResponseHook): STATE = {
        givenProcessors match {
          case Some(list) => if (processors.get(httpHook).size == givenProcessors.get.size) STATE.ABORT else STATE.CONTINUE
          case None => STATE.CONTINUE
        }
      }

      def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {
        responseBuilder.accumulate(responseStatus)
        processResponse(StatusReceived, responseStatus.getStatusCode)
      }

      def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
        responseBuilder.accumulate(headers)
        processResponse(HeadersReceived, headers.getHeaders) // Ici c'est compliqué...
      }

      def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
        responseBuilder.accumulate(bodyPart)
        STATE.CONTINUE
      }

      def onCompleted(): Response = {
        val startTime: Long = System.nanoTime()
        processResponse(CompletePageReceived, responseBuilder.build.getResponseBody)
        println("ResponseReceived")
        next.execute(contextBuilder setElapsedActionTime (System.nanoTime() - startTime) build)
        null
      }

      def onThrowable(throwable: Throwable) = {
        throwable.printStackTrace
      }
    })
  }
}