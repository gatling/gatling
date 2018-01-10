/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

import java.util.Locale

import io.gatling.http.HeaderNames

private[scenario] object ProtocolDefinition {

  val BaseHeaders = Map(
    HeaderNames.Accept.toLowerCase(Locale.ROOT) -> "acceptHeader",
    HeaderNames.AcceptCharset.toLowerCase(Locale.ROOT) -> "acceptCharsetHeader",
    HeaderNames.AcceptEncoding.toLowerCase(Locale.ROOT) -> "acceptEncodingHeader",
    HeaderNames.AcceptLanguage.toLowerCase(Locale.ROOT) -> "acceptLanguageHeader",
    HeaderNames.Authorization.toLowerCase(Locale.ROOT) -> "authorizationHeader",
    HeaderNames.Connection.toLowerCase(Locale.ROOT) -> "connectionHeader",
    HeaderNames.ContentType.toLowerCase(Locale.ROOT) -> "contentTypeHeader",
    HeaderNames.DNT.toLowerCase(Locale.ROOT) -> "doNotTrackHeader",
    HeaderNames.UserAgent.toLowerCase(Locale.ROOT) -> "userAgentHeader",
    HeaderNames.UpgradeInsecureRequests.toLowerCase(Locale.ROOT) -> "upgradeInsecureRequestsHeader"
  )
}

private[scenario] case class ProtocolDefinition(baseUrl: String, headers: Map[String, String])
