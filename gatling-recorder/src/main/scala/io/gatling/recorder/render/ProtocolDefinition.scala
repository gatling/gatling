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

package io.gatling.recorder.render

import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpHeaderNames, HttpHeaders }

private[render] object ProtocolDefinition {
  // use HttpHeaders because keys are case insensitive
  val BaseHeadersAndProtocolMethods: HttpHeaders = new DefaultHttpHeaders(false)
    .add(HttpHeaderNames.ACCEPT, "acceptHeader")
    .add(HttpHeaderNames.ACCEPT_CHARSET, "acceptCharsetHeader")
    .add(HttpHeaderNames.ACCEPT_ENCODING, "acceptEncodingHeader")
    .add(HttpHeaderNames.ACCEPT_LANGUAGE, "acceptLanguageHeader")
    .add(HttpHeaderNames.AUTHORIZATION, "authorizationHeader")
    .add(HttpHeaderNames.CONNECTION, "connectionHeader")
    .add(HttpHeaderNames.CONTENT_TYPE, "contentTypeHeader")
    .add(HttpHeaderNames.DNT, "doNotTrackHeader")
    .add(HttpHeaderNames.ORIGIN, "originHeader")
    .add(HttpHeaderNames.USER_AGENT, "userAgentHeader")
    .add(HttpHeaderNames.UPGRADE_INSECURE_REQUESTS, "upgradeInsecureRequestsHeader")
}

private[render] final case class ProtocolDefinition(baseUrl: String, headers: HttpHeaders)
