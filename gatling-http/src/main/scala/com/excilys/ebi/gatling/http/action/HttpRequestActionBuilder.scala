/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
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
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.util.StringHelper.interpolate
import com.excilys.ebi.gatling.http.request.HttpRequest
import com.excilys.ebi.gatling.http.request.builder.GetHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.builder.PostHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.builder.DeleteHttpRequestBuilder
import com.excilys.ebi.gatling.http.request.builder.PutHttpRequestBuilder
import akka.actor.Actor._
import com.excilys.ebi.gatling.http.request.builder.AbstractHttpRequestBuilder
import com.excilys.ebi.gatling.http.check.HttpCheckBuilder
import akka.actor.ActorRef
import com.excilys.ebi.gatling.core.config.ProtocolConfigurationRegistry
import com.excilys.ebi.gatling.http.config.HttpProtocolConfiguration
import com.excilys.ebi.gatling.core.check.CheckBuilder
import com.excilys.ebi.gatling.http.check.HttpCheck
import com.ning.http.client.Response

/**
 * HttpRequestActionBuilder class companion
 */
object HttpRequestActionBuilder {
	/**
	 * This method is used in DSL to declare a new HTTP request
	 */
	def http(requestName: String) = new HttpRequestActionBuilder(requestName, null, null, None)
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
class HttpRequestActionBuilder(val requestName: String, request: HttpRequest, next: ActorRef, checks: Option[List[HttpCheck[_]]])
		extends AbstractActionBuilder {

	/**
	 * Adds givenProcessors to builder
	 *
	 * @param givenProcessors the processors specified by the user
	 * @return a new builder with givenProcessors set
	 */
	private[http] def withProcessors(givenChecks: Seq[HttpCheck[_]]) = {
		new HttpRequestActionBuilder(requestName, request, next, Some(givenChecks.toList ::: checks.getOrElse(Nil)))
	}

	private[http] def withRequest(request: HttpRequest) = new HttpRequestActionBuilder(requestName, request, next, checks)

	private[gatling] def withNext(next: ActorRef) = new HttpRequestActionBuilder(requestName, request, next, checks)

	private[gatling] def build(protocolConfigurationRegistry: ProtocolConfigurationRegistry) = {
		actorOf(new HttpRequestAction(next, request, checks, protocolConfigurationRegistry.getProtocolConfiguration(HttpProtocolConfiguration.HTTP_PROTOCOL_TYPE).as[HttpProtocolConfiguration])).start
	}

	/**
	 * Starts the definition of an HTTP request with word DELETE
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations session keys for interpolation
	 */
	def delete(url: String) = new DeleteHttpRequestBuilder(this, interpolate(url), Nil, Map(), None, None, None)

	/**
	 * Starts the definition of an HTTP request with word DELETE
	 *
	 * @param f the function returning the url of this request
	 */
	def delete(f: Session => String) = new DeleteHttpRequestBuilder(this, f, Nil, Map(), None, None, None)

	/**
	 * Starts the definition of an HTTP request with word GET
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations session keys for interpolation
	 */
	def get(url: String) = new GetHttpRequestBuilder(this, interpolate(url), Nil, Map(), None, None)

	/**
	 * Starts the definition of an HTTP request with word GET
	 *
	 * @param f the function returning the url of this request
	 */
	def get(f: Session => String) = new GetHttpRequestBuilder(this, f, Nil, Map(), None, None)

	/**
	 * Starts the definition of an HTTP request with word POST
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations session keys for interpolation
	 */
	def post(url: String) = new PostHttpRequestBuilder(this, interpolate(url), Nil, Nil, Map(), None, None, None, None)

	/**
	 * Starts the definition of an HTTP request with word POST
	 *
	 * @param f the function returning the url of this request
	 */
	def post(f: Session => String) = new PostHttpRequestBuilder(this, f, Nil, Nil, Map(), None, None, None, None)

	/**
	 * Starts the definition of an HTTP request with word PUT
	 *
	 * @param url the url on which this request will be made
	 * @param interpolations session keys for interpolation
	 */
	def put(url: String) = new PutHttpRequestBuilder(this, interpolate(url), Nil, Map(), None, None, None)

	/**
	 * Starts the definition of an HTTP request with word PUT
	 *
	 * @param f the function returning the url of this request
	 */
	def put(f: Session => String) = new PutHttpRequestBuilder(this, f, Nil, Map(), None, None, None)
}

