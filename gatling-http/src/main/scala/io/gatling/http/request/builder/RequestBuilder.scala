/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request.builder

import java.net.{ InetAddress, URI }

import com.ning.http.client.{ ProxyServer, Realm }
import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.config.Proxy
import io.gatling.core.session.Expression
import io.gatling.core.session.el.EL
import io.gatling.http.ahc.ProxyConverter
import io.gatling.http.util.HttpHelper

case class CommonAttributes(
	requestName: Expression[String],
	method: String,
	urlOrURI: Either[Expression[String], URI],
	useRawUrl: Option[Boolean] = None,
	queryParams: List[HttpParam] = Nil,
	headers: Map[String, Expression[String]] = Map.empty,
	realm: Option[Expression[Realm]] = None,
	virtualHost: Option[Expression[String]] = None,
	address: Option[InetAddress] = None,
	proxy: Option[ProxyServer] = None,
	secureProxy: Option[ProxyServer] = None)

abstract class RequestBuilder[B <: RequestBuilder[B]](val commonAttributes: CommonAttributes) extends StrictLogging {

	private[http] def newInstance(commonAttributes: CommonAttributes): B

	def queryParam(key: Expression[String], value: Expression[Any]): B = queryParam(SimpleParam(key, value))
	def multivaluedQueryParam(key: Expression[String], values: Expression[Seq[Any]]): B = queryParam(MultivaluedParam(key, values))
	def queryParamsSequence(seq: Expression[Seq[(String, Any)]]): B = queryParam(ParamSeq(seq))
	def queryParamsMap(map: Expression[Map[String, Any]]): B = queryParam(ParamMap(map))
	private def queryParam(param: HttpParam): B = newInstance(commonAttributes.copy(queryParams = param :: commonAttributes.queryParams))

	/**
	 * Adds a header to the request
	 *
	 * @param header the header to add, eg: ("Content-Type", "application/json")
	 */
	def header(name: String, value: Expression[String]): B = newInstance(commonAttributes.copy(headers = commonAttributes.headers + (name -> value)))

	/**
	 * Adds several headers to the request at the same time
	 *
	 * @param newHeaders a scala map containing the headers to add
	 */
	def headers(newHeaders: Map[String, String]): B = newInstance(commonAttributes.copy(headers = commonAttributes.headers ++ newHeaders.mapValues(_.el[String])))

	/**
	 * Adds BASIC authentication to the request
	 *
	 * @param username the username needed
	 * @param password the password needed
	 */
	def basicAuth(username: Expression[String], password: Expression[String]): B = authRealm(HttpHelper.buildBasicAuthRealm(username, password))
	def digestAuth(username: Expression[String], password: Expression[String]) = authRealm(HttpHelper.buildDigestAuthRealm(username, password))
	def authRealm(realm: Expression[Realm]): B = newInstance(commonAttributes.copy(realm = Some(realm)))

	/**
	 * @param virtualHost a virtual host to override default compute one
	 */
	def virtualHost(virtualHost: Expression[String]): B = newInstance(commonAttributes.copy(virtualHost = Some(virtualHost)))

	def address(address: InetAddress): B = newInstance(commonAttributes.copy(address = Some(address)))

	def useRawUrl: B = newInstance(commonAttributes.copy(useRawUrl = Some(true)))

	def proxy(httpProxy: Proxy): B = newInstance(commonAttributes.copy(proxy = Some(httpProxy.proxyServer), secureProxy = httpProxy.secureProxyServer))
}
