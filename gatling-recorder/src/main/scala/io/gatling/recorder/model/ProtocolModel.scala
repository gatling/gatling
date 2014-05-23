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
package io.gatling.recorder.model

import scala.annotation.tailrec
import scala.collection.immutable.SortedMap
import java.util.concurrent.atomic.AtomicReference

import io.gatling.recorder.config.RecorderConfiguration
import io.gatling.http.HeaderNames

object ProtocolModel {

  private val baseHeadersMethodMap = Map(
    HeaderNames.ACCEPT -> "acceptHeader",
    HeaderNames.ACCEPT_CHARSET -> "acceptCharsetHeader",
    HeaderNames.ACCEPT_ENCODING -> "acceptEncodingHeader",
    HeaderNames.ACCEPT_LANGUAGE -> "acceptLanguageHeader",
    HeaderNames.AUTHORIZATION -> "authorizationHeader",
    HeaderNames.CONNECTION -> "connection",
    // HeaderNames.CONTENT_TYPE -> "contentTypeHeader", // the content type header is never going to be common to many reqs
    HeaderNames.DO_NOT_TRACK -> "doNotTrackHeader",
    HeaderNames.USER_AGENT -> "userAgentHeader")

  def apply(model: SimulationModel)(implicit config: RecorderConfiguration): ProtocolModel = {

    val requestsSet = model.getRequests

    val proxyCredentials = model.proxyCredentials.get match {
      case a: String => Some(a)
      case _ => None
    }

    val scenarioElements1 = model.getRequests.toSeq

    val filteredHeaders = Set(HeaderNames.COOKIE, HeaderNames.CONTENT_LENGTH, HeaderNames.HOST) ++
      (if (config.http.automaticReferer) Set(HeaderNames.REFERER) else Set.empty)

    def getBaseUrl(scenarioElements: Seq[RequestModel]): String = {
      val urlsOccurrences = scenarioElements.collect {
        case reqElm: RequestModel => reqElm.baseUrl
      }.groupBy(identity).mapValues(_.size).toSeq

      urlsOccurrences.maxBy(_._2)._1
    }

    def getBaseHeaders(scenarioElements: Seq[RequestModel]): Map[String, String] = {
      def addHeader(appendTo: Map[String, String], headerName: String): Map[String, String] =
        getMostFrequentHeaderValue(scenarioElements, headerName)
          .map(headerValue => appendTo + (headerName -> headerValue))
          .getOrElse(appendTo)

      @tailrec
      def resolveBaseHeaders(headers: Map[String, String], headerNames: List[String]): Map[String, String] = headerNames match {
        case Nil => headers
        case headerName :: others => resolveBaseHeaders(addHeader(headers, headerName), others)
      }

      resolveBaseHeaders(Map.empty, baseHeadersMethodMap.keySet.toList)
    }

    def getMostFrequentHeaderValue(scenarioElements: Seq[RequestModel], headerName: String): Option[String] = {
      val headers = scenarioElements.flatMap {
        case reqElm: RequestModel => reqElm.headers.collect { case (name, value) if name == headerName => value }
        case _ => Nil
      }

      if (headers.isEmpty) None
      else {
        val headersValuesOccurrences = headers.groupBy(identity).mapValues(_.size).toSeq
        val mostFrequentValue = headersValuesOccurrences.maxBy(_._2)._1
        Some(mostFrequentValue)
      }
    }

    val baseHeaders = getBaseHeaders(model.getRequests.toSeq)

    def headers: Map[String, Seq[(String, String)]] = {

      // Map[String, List[(String, String)]] ==> map where key==request_idenfier, value is a list of common headers
      @tailrec
      def generateHeaders(elements: Seq[RequestModel], headers: Map[String, List[(String, String)]]): Map[String, List[(String, String)]] = elements match {
        case Seq() => headers
        case element +: others =>
          val acceptedHeaders = element.headers.toList
            .filterNot {
              case (headerName, headerValue) => filteredHeaders.contains(headerName) || baseHeaders.get(headerName).exists(_ == headerValue)
            }
            .sortBy(_._1)

          val newHeaders = if (acceptedHeaders.isEmpty) {
            element.header_identifier = None
            headers

          } else {
            val headersSeq = headers.toSeq
            headersSeq.indexWhere {
              case (id, existingHeaders) => existingHeaders == acceptedHeaders
            } match {
              case -1 =>
                element.header_identifier = Some(element.identifier) //.id
                headers + (element.identifier -> acceptedHeaders)
              case index =>
                element.header_identifier = Some(headersSeq(index)._1)
                headers
            }
          }

          generateHeaders(others, newHeaders)
      }

      SortedMap(generateHeaders(model.getRequests.toSeq, Map.empty).toSeq: _*)
    }

    val headers1 = headers

    apply(getBaseUrl(model.getRequests.toSeq), baseHeaders, headers1, proxyCredentials)
  }
}

case class ProtocolModel(baseUrl: String, baseHeaders: Map[String, String], headers: Map[String, Seq[(String, String)]], proxyCredentials: Option[String])(implicit config: RecorderConfiguration) { // (baseUrl: String, headers: Map[String, String]) {

  val proxy_outgoing_host = config.proxy.outgoing.host
  val proxy_outgoing_port = config.proxy.outgoing.port
  val proxy_outgoing_sslPort = config.proxy.outgoing.sslPort

  val http_followRedirect = config.http.followRedirect
  val http_fetchHtmlResources = config.http.fetchHtmlResources
  val domain_filters = config.filters
  val http_automaticReferer = config.http.automaticReferer

}
