/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.recorder.scenario

import io.gatling.http.HeaderNames

object ProtocolDefinition {

  val baseHeaders = Map(
    HeaderNames.ACCEPT -> "acceptHeader",
    HeaderNames.ACCEPT_CHARSET -> "acceptCharsetHeader",
    HeaderNames.ACCEPT_ENCODING -> "acceptEncodingHeader",
    HeaderNames.ACCEPT_LANGUAGE -> "acceptLanguageHeader",
    HeaderNames.AUTHORIZATION -> "authorizationHeader",
    HeaderNames.CONNECTION -> "connection",
    HeaderNames.CONTENT_TYPE -> "contentTypeHeader",
    HeaderNames.DO_NOT_TRACK -> "doNotTrackHeader",
    HeaderNames.USER_AGENT -> "userAgentHeader")
}

case class ProtocolDefinition(baseUrl: String, headers: Map[String, String])
