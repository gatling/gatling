package com.excilys.ebi.gatling.http.action.builder

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.scenario.builder.ScenarioBuilder
import com.excilys.ebi.gatling.core.util.StringHelper._
import com.excilys.ebi.gatling.http.action.HttpRequestAction
import com.excilys.ebi.gatling.http.processor.builder.HttpProcessorBuilder
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.request.builder.GetHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.builder.PostHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.builder.DeleteHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.builder.PutHttpRequestBuilder
import com.excilys.ebi.gatling.http.processor.capture.builder.AbstractHttpCaptureBuilder
import com.excilys.ebi.gatling.http.processor.check.builder.HttpCheckBuilder
import akka.actor.TypedActor
import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestBuilder

object HttpRequestActionBuilder {
	def http(requestName: String) = new HttpRequestActionBuilder(requestName, None, None, None, Some(Nil))
}

class HttpRequestActionBuilder(val requestName: String, val request: Option[HttpRequest], val nextAction: Option[Action], val processorBuilders: Option[List[HttpProcessorBuilder]], val groups: Option[List[String]])
		extends AbstractActionBuilder {

	def getRequestName = requestName

	private[http] def withProcessors(givenProcessors: Seq[HttpProcessorBuilder]) = {
		logger.debug("Adding Processors")
		new HttpRequestActionBuilder(requestName, request, nextAction, Some(givenProcessors.toList ::: processorBuilders.getOrElse(Nil)), groups)
	}

	def capture(captureBuilders: AbstractHttpCaptureBuilder[_]*) = withProcessors(captureBuilders)

	def check(checkBuilders: HttpCheckBuilder[_]*) = withProcessors(checkBuilders)

	def withRequest(request: HttpRequest) = new HttpRequestActionBuilder(requestName, Some(request), nextAction, processorBuilders, groups)

	def withNext(next: Action) = new HttpRequestActionBuilder(requestName, request, Some(next), processorBuilders, groups)

	def inGroups(groups: List[String]) = new HttpRequestActionBuilder(requestName, request, nextAction, processorBuilders, Some(groups))

	def build: Action = {
		logger.debug("Building HttpRequestAction with request {}", request.get)
		TypedActor.newInstance(classOf[Action], new HttpRequestAction(nextAction.get, request.get, processorBuilders, groups.get))
	}

	def delete(url: String, interpolations: String*) =
		new DeleteHttpRequestBuilder(this, Some((c: Context) => interpolateString(c, url, interpolations)), Some(Map()), Some(Map()), None, None, None)

	def delete(f: Context => String) =
		new DeleteHttpRequestBuilder(this, Some(f), Some(Map()), Some(Map()), None, None, None)

	def get(url: String, interpolations: String*) =
		new GetHttpRequestBuilder(this, Some((c: Context) => interpolateString(c, url, interpolations)), Some(Map()), Some(Map()), None, None)

	def get(f: Context => String) =
		new GetHttpRequestBuilder(this, Some(f), Some(Map()), Some(Map()), None, None)

	def post(url: String, interpolations: String*) =
		new PostHttpRequestBuilder(this, Some((c: Context) => interpolateString(c, url, interpolations)), Some(Map()), Some(Map()), Some(Map()), None, None, None)

	def post(f: Context => String) =
		new PostHttpRequestBuilder(this, Some(f), Some(Map()), Some(Map()), Some(Map()), None, None, None)

	def put(url: String, interpolations: String*) =
		new PutHttpRequestBuilder(this, Some((c: Context) => interpolateString(c, url, interpolations)), Some(Map()), Some(Map()), None, None, None)

	def put(f: Context => String) =
		new PutHttpRequestBuilder(this, Some(f), Some(Map()), Some(Map()), None, None, None)
}

