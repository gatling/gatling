/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.core.session._
import io.gatling.http.client.uri.Uri

import io.netty.handler.codec.http.HttpMethod

/**
 * @param requestName
 *   the name of the request
 */
final class Http(requestName: Expression[String]) {
  def get(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.GET, Left(url))
  private[http] def get(uri: Uri): HttpRequestBuilder = httpRequest(HttpMethod.GET, Right(uri))
  def put(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.PUT, Left(url))
  def post(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.POST, Left(url))
  def patch(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.PATCH, Left(url))
  def head(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.HEAD, Left(url))
  def delete(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.DELETE, Left(url))
  def options(url: Expression[String]): HttpRequestBuilder = httpRequest(HttpMethod.OPTIONS, Left(url))
  def httpRequest(method: Expression[String], url: Expression[String]): HttpRequestBuilder =
    HttpRequestBuilder(requestName, method.map(HttpMethod.valueOf), Left(url))
  private def httpRequest(method: HttpMethod, urlOrURI: Either[Expression[String], Uri]): HttpRequestBuilder =
    HttpRequestBuilder(requestName, method.expressionSuccess, urlOrURI)
}
