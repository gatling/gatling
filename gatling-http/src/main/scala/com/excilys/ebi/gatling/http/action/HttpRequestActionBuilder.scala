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

/**
 * HttpRequestActionBuilder class companion
 */
object HttpRequestActionBuilder {
	/**
	 * This method is used in DSL to declare a new HTTP request
	 */
	def http(requestName: String) = new HttpRequestActionBuilder(requestName, null, null, None, Some(Nil), None)
}

/**
 * Builder for HttpRequestActionBuilder
 *
 * @constructor creates an HttpRequestActionBuilder
 * @param requestName the name of the request
 * @param request the actual HTTP request that will be sent
 * @param next the next action to be executed
 * @param processorBuilders
 */
class HttpRequestActionBuilder(val requestName: String, request: HttpRequest, next: Action, processorBuilders: Option[List[HttpCheckBuilder[_]]], groups: Option[List[String]], feeder: Option[Feeder])
		extends AbstractActionBuilder {

	/**
	 * Adds givenProcessors to builder
	 *
	 * @param givenProcessors the processors specified by the user
	 * @return a new builder with givenProcessors set
	 */
	private[http] def withProcessors(givenProcessors: Seq[HttpCheckBuilder[_]]) = {
		logger.debug("Adding Processors")
		new HttpRequestActionBuilder(requestName, request, next, Some(givenProcessors.toList ::: processorBuilders.getOrElse(Nil)), groups, feeder)
	}

	/**
	 * Adds a feeder to builder
	 *
	 * @param feeder the feeder to add
	 * @return a new builder with feeder set
	 */
	def feeder(feeder: Feeder) = new HttpRequestActionBuilder(requestName, request, next, processorBuilders, groups, Some(feeder))

	def withRequest(request: HttpRequest) = new HttpRequestActionBuilder(requestName, request, next, processorBuilders, groups, feeder)

	def withNext(next: Action) = new HttpRequestActionBuilder(requestName, request, next, processorBuilders, groups, feeder)

	def inGroups(groups: List[String]) = new HttpRequestActionBuilder(requestName, request, next, processorBuilders, Some(groups), feeder)

	def build: Action = {
		logger.debug("Building HttpRequestAction with request {}", request)
		TypedActor.newInstance(classOf[Action], new HttpRequestAction(next, request, processorBuilders, groups.get, feeder))
	}

	/**
	 * Starts the definition of an HTTP request with word DELETE
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations context keys for interpolation
	 */
	def delete(url: String) = new DeleteHttpRequestBuilder(this, interpolate(url), Nil, Map(), None, None, None)

	/**
	 * Starts the definition of an HTTP request with word DELETE
	 *
	 * @param f the function returning the url of this request
	 */
	def delete(f: Context => String) = new DeleteHttpRequestBuilder(this, f, Nil, Map(), None, None, None)

	/**
	 * Starts the definition of an HTTP request with word GET
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations context keys for interpolation
	 */
	def get(url: String) = new GetHttpRequestBuilder(this, interpolate(url), Nil, Map(), None, None)

	/**
	 * Starts the definition of an HTTP request with word GET
	 *
	 * @param f the function returning the url of this request
	 */
	def get(f: Context => String) = new GetHttpRequestBuilder(this, f, Nil, Map(), None, None)

	/**
	 * Starts the definition of an HTTP request with word POST
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations context keys for interpolation
	 */
	def post(url: String) = new PostHttpRequestBuilder(this, interpolate(url), Nil, Nil, Map(), None, None, None)

	/**
	 * Starts the definition of an HTTP request with word POST
	 *
	 * @param f the function returning the url of this request
	 */
	def post(f: Context => String) = new PostHttpRequestBuilder(this, f, Nil, Nil, Map(), None, None, None)

	/**
	 * Starts the definition of an HTTP request with word PUT
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations context keys for interpolation
	 */
	def put(url: String) = new PutHttpRequestBuilder(this, interpolate(url), Nil, Map(), None, None, None)

	/**
	 * Starts the definition of an HTTP request with word PUT
	 *
	 * @param f the function returning the url of this request
	 */
	def put(f: Context => String) = new PutHttpRequestBuilder(this, f, Nil, Map(), None, None, None)
}

