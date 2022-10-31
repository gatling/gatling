/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.http.engine.response

import io.gatling.commons.stats.{ KO, Status }
import io.gatling.commons.util.StringHelper.Eol
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.http.response.{ HttpResult, Response }
import io.gatling.http.util._
import io.gatling.netty.util.StringBuilderPool

import com.typesafe.scalalogging.StrictLogging
import io.gatling.http.util.HttpHelper.isText

sealed abstract class StatsProcessor extends StrictLogging {
  def reportStats(
      fullRequestName: String,
      session: Session,
      status: Status,
      result: HttpResult,
      errorMessage: Option[String]
  ): Unit = {
    logTx(fullRequestName, session, status, result, errorMessage)
    reportStats0(fullRequestName, session, status, result, errorMessage)
  }

  protected def reportStats0(
      fullRequestName: String,
      session: Session,
      status: Status,
      result: HttpResult,
      errorMessage: Option[String]
  ): Unit

  private val loggingStringBuilderPool = new StringBuilderPool
  private def logTx(
      fullRequestName: String,
      session: Session,
      status: Status,
      result: HttpResult,
      errorMessage: Option[String]
  ): Unit = {

    var response_object: Response = null
    result match {
      case response: Response => response_object = response
      case _                  =>
    }

    val request = result.request

    var url = request.getUri.toUrl
    if (url.isEmpty) {
      url = "[]"
    }
    val method = request.getMethod
    val status_code = response_object.status.code()
    val response_time = result.endTimestamp - result.startTimestamp
    val build_id = System.getenv("build_id")
    val lg_id = System.getenv("lg_id")
    val user_id = session.userId
    val test_type = System.getenv("test_type")
    val simulation_name = System.getenv("simulation_name")
    val env = System.getenv("env")
    val time = System.currentTimeMillis()

    def comparison_dump = {
      val comparison = StringBuilderPool.DEFAULT.get()
      comparison
        .append(s"$time")
        .append("\t")
        .append(s"$simulation_name")
        .append("\t")
        .append(s"$fullRequestName")
        .append("\t")
        .append(s"$response_time")
        .append("\t")
        .append(s"$method")
        .append("\t")
        .append(s"$status")
        .append("\t")
        .append(s"$status_code")
        .append("\t")
        .append(s"$user_id")
        .append("\t")
        .append(s"$env")
        .append("\t")
        .append(s"$test_type")
        .append("\t")
        .append(s"$build_id")
        .append("\t")
        .append(s"$lg_id")
      comparison.toString
    }

    def error_dump = {
      val headers = request.getHeaders.entries().toString.replaceAll("\t", "")
      var params = "["
      val query_params = request.getUri.getQuery
      if (query_params != null) {
        params += query_params
      }
      params += "]"

      val error_key = s"$fullRequestName" + s"_$method" + s"_$status_code"
      var response_body = "[]"
      if (response_object.body.length > 0) {
        if (isText(response_object.headers)) {
          response_body = response_object.body.string.replaceAll("\n", "").replaceAll("\t", "")
        } else {
          response_body = "<<<BINARY CONTENT>>>"
        }
      }
      val buff = StringBuilderPool.DEFAULT.get()
      buff.append(s"""Error key: $error_key""").append("\t").append(s"""Request name: $fullRequestName""").append("\t")
      buff.append(s"""Method: $method""").append("\t").append(s"""Response code: $status_code""").append("\t")
      buff.append(s"""URL: $url""").append("\t").append(s"""Error message: ${errorMessage.getOrElse("")}""").append("\t")
      buff.append(s"""Request params: $params""").append("\t").append(s"""Headers: $headers""").append("\t")
      buff.append(s"""Response body: ${response_body.replaceAll("\"", "").replaceAll("\'", "").replaceAll("'", "")}""")
      buff.append("\t")
      buff.toString
    }

    logger.trace(comparison_dump)

//    def dump = {
//      loggingStringBuilderPool
//        .get()
//        .append(Eol)
//        .appendWithEol(">>>>>>>>>>>>>>>>>>>>>>>>>>")
//        .appendWithEol("Request:")
//        .appendWithEol(s"$fullRequestName: $status ${errorMessage.getOrElse("")}")
//        .appendWithEol("=========================")
//        .appendWithEol("Session:")
//        .append(session)
//        .append(Eol)
//        .appendWithEol("=========================")
//        .appendWithEol("HTTP request:")
//        .appendRequest(result.request)
//        .appendWithEol("=========================")
//        .appendWithEol("HTTP response:")
//        .appendResponse(result)
//        .append("<<<<<<<<<<<<<<<<<<<<<<<<<")
//        .toString
//    }

    if (status == KO) {
//      logger.debug(s"Request '$fullRequestName' failed for user ${session.userId}: ${errorMessage.getOrElse("")}")
//      if (!HttpTracing.IS_HTTP_TRACE_ENABLED) {
//        logger.debug(dump)
//      }
      logger.error(error_dump)
    }

//    logger.trace(dump)
  }
}

object NoopStatsProcessor extends StatsProcessor {
  override protected def reportStats0(
      fullRequestName: String,
      session: Session,
      status: Status,
      result: HttpResult,
      errorMessage: Option[String]
  ): Unit = {}
}

final class DefaultStatsProcessor(
    statsEngine: StatsEngine
) extends StatsProcessor {

  override def reportStats0(
      fullRequestName: String,
      session: Session,
      status: Status,
      result: HttpResult,
      errorMessage: Option[String]
  ): Unit =
    statsEngine.logResponse(
      session.scenario,
      session.groups,
      fullRequestName,
      result.startTimestamp,
      result.endTimestamp,
      status,
      result match {
        case response: Response => Some(Integer.toString(response.status.code))
        case _                  => None
      },
      errorMessage
    )
}
