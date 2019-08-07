/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.recorder.scenario

import io.gatling.http.HeaderNames

import io.netty.handler.codec.http.{ DefaultHttpHeaders, HttpHeaders }

private[scenario] object ProtocolDefinition {

  // use HttpHeaders because keys are case insensitive
  val BaseHeadersAndProtocolMethods: HttpHeaders = new DefaultHttpHeaders(false)
    .add(HeaderNames.Accept, "acceptHeader")
    .add(HeaderNames.AcceptCharset, "acceptCharsetHeader")
    .add(HeaderNames.AcceptEncoding, "acceptEncodingHeader")
    .add(HeaderNames.AcceptLanguage, "acceptLanguageHeader")
    .add(HeaderNames.Authorization, "authorizationHeader")
    .add(HeaderNames.Connection, "connectionHeader")
    .add(HeaderNames.ContentType, "contentTypeHeader")
    .add(HeaderNames.DNT, "doNotTrackHeader")
    .add(HeaderNames.UserAgent, "userAgentHeader")
    .add(HeaderNames.UpgradeInsecureRequests, "upgradeInsecureRequestsHeader")
}

private[scenario] final case class ProtocolDefinition(baseUrl: String, headers: HttpHeaders)
