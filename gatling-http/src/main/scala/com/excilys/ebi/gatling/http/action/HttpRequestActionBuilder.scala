/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.action

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.action.builder.AbstractActionBuilder
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.util.StringHelper._
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.request.builder.GetHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.builder.PostHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.builder.DeleteHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.builder.PutHttpRequestBuilder
import akka.actor.TypedActor
import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestBuilder
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder

object HttpRequestActionBuilder {
	def http(requestName: String) = new HttpRequestActionBuilder(requestName, None, None, None, Some(Nil), None)
}

class HttpRequestActionBuilder(val requestName: String, val request: Option[HttpRequest], val nextAction: Option[Action], val processorBuilders: Option[List[HttpCheckBuilder[_]]], val groups: Option[List[String]], val feeder: Option[Feeder])
		extends AbstractActionBuilder {

	private[http] def withProcessors(givenProcessors: Seq[HttpCheckBuilder[_]]) = {
		logger.debug("Adding Processors")
		new HttpRequestActionBuilder(requestName, request, nextAction, Some(givenProcessors.toList ::: processorBuilders.getOrElse(Nil)), groups, feeder)
	}

	private[http] def withFeeder(feeder: Feeder) = new HttpRequestActionBuilder(requestName, request, nextAction, processorBuilders, groups, Some(feeder))

	def capture(captureBuilders: HttpCheckBuilder[_]*) = withProcessors(captureBuilders)

	def check(checkBuilders: HttpCheckBuilder[_]*) = withProcessors(checkBuilders)

	def feeder(feeder: Feeder) = withFeeder(feeder)

	def withRequest(request: HttpRequest) = new HttpRequestActionBuilder(requestName, Some(request), nextAction, processorBuilders, groups, feeder)

	def withNext(next: Action) = new HttpRequestActionBuilder(requestName, request, Some(next), processorBuilders, groups, feeder)

	def inGroups(groups: List[String]) = new HttpRequestActionBuilder(requestName, request, nextAction, processorBuilders, Some(groups), feeder)

	def build: Action = {
		logger.debug("Building HttpRequestAction with request {}", request.get)
		TypedActor.newInstance(classOf[Action], new HttpRequestAction(nextAction.get, request.get, processorBuilders, groups.get, feeder))
	}

	def delete(url: String, interpolations: String*) = new DeleteHttpRequestBuilder(this, Some((c: Context) => interpolateString(c, url, interpolations)), Some(Map()), Some(Map()), None, None, None)

	def delete(f: Context => String) = new DeleteHttpRequestBuilder(this, Some(f), Some(Map()), Some(Map()), None, None, None)

	def get(url: String, interpolations: String*) = new GetHttpRequestBuilder(this, Some((c: Context) => interpolateString(c, url, interpolations)), Some(Map()), Some(Map()), None, None)

	def get(f: Context => String) = new GetHttpRequestBuilder(this, Some(f), Some(Map()), Some(Map()), None, None)

	def post(url: String, interpolations: String*) = new PostHttpRequestBuilder(this, Some((c: Context) => interpolateString(c, url, interpolations)), Some(Map()), Some(Map()), Some(Map()), None, None, None)

	def post(f: Context => String) = new PostHttpRequestBuilder(this, Some(f), Some(Map()), Some(Map()), Some(Map()), None, None, None)

	def put(url: String, interpolations: String*) = new PutHttpRequestBuilder(this, Some((c: Context) => interpolateString(c, url, interpolations)), Some(Map()), Some(Map()), None, None, None)

	def put(f: Context => String) = new PutHttpRequestBuilder(this, Some(f), Some(Map()), Some(Map()), None, None, None)
}

