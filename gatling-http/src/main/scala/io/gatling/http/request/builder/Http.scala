/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.request.builder

import io.gatling.core.session.Expression
import io.gatling.http.client.uri.Uri

import io.netty.handler.codec.http.HttpMethod

/**
 * @param requestName the name of the request
 */
final case class Http(requestName: Expression[String]) {

  def get(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.GET, url)
  def get(uri: Uri): HttpRequestBuilder = httpRequest(HttpMethod.GET, Right(uri))
  def put(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.PUT, url)
  def post(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.POST, url)
  def patch(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.PATCH, url)
  def head(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.HEAD, url)
  def delete(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.DELETE, url)
  def options(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.OPTIONS, url)
  def httpRequest(method: String, url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.valueOf(method), Left(url))
  def httpRequest(method: HttpMethod, url: Expression[String]): HttpRequestBuilder = httpRequest(method, Left(url))
  def httpRequest(method: HttpMethod, urlOrURI: Either[Expression[String], Uri]): HttpRequestBuilder =
    new HttpRequestBuilder(CommonAttributes(requestName, method, urlOrURI), HttpAttributes.Empty)
}
