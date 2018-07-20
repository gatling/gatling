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

package io.gatling.http.engine.response

import java.nio.charset.Charset

import io.gatling.commons.stats.{ KO, Status }
import io.gatling.commons.util.StringHelper.Eol
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.http.client.Request
import io.gatling.http.response.{ HttpResult, Response }
import io.gatling.http.util._
import io.gatling.netty.util.ahc.StringBuilderPool

import com.typesafe.scalalogging.StrictLogging

sealed trait StatsProcessor {
  def reportStats(
    fullRequestName: String,
    request:         Request,
    session:         Session,
    status:          Status,
    result:          HttpResult,
    errorMessage:    Option[String]
  ): Unit
}

object NoopStatsProcessor extends StatsProcessor {
  override def reportStats(
    fullRequestName: String,
    request:         Request,
    session:         Session,
    status:          Status,
    result:          HttpResult,
    errorMessage:    Option[String]
  ): Unit = {}
}

class DefaultStatsProcessor(
    charset:     Charset,
    statsEngine: StatsEngine
) extends StatsProcessor with StrictLogging {

  override def reportStats(
    fullRequestName: String,
    request:         Request,
    session:         Session,
    status:          Status,
    result:          HttpResult,
    errorMessage:    Option[String]
  ): Unit = {
    logTx0(fullRequestName, request, session, status, result, errorMessage, charset)

    statsEngine.logResponse(
      session,
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

  private def logTx0(
    fullRequestName: String,
    request:         Request,
    session:         Session,
    status:          Status,
    result:          HttpResult,
    errorMessage:    Option[String] = None,
    charset:         Charset
  ): Unit = {
    def dump = {
      // hack: pre-cache url because it would reset the StringBuilder
      // FIXME isn't this url already built when sending the request?
      request.getUri.toUrl
      StringBuilderPool.DEFAULT.get().append(Eol)
        .appendWithEol(">>>>>>>>>>>>>>>>>>>>>>>>>>")
        .appendWithEol("Request:")
        .appendWithEol(s"$fullRequestName: $status ${errorMessage.getOrElse("")}")
        .appendWithEol("=========================")
        .appendWithEol("Session:")
        .appendWithEol(session)
        .appendWithEol("=========================")
        .appendWithEol("HTTP request:")
        .appendRequest(request, result, charset)
        .appendWithEol("=========================")
        .appendWithEol("HTTP response:")
        .appendResponse(result)
        .append("<<<<<<<<<<<<<<<<<<<<<<<<<")
        .toString
    }

    if (status == KO) {
      logger.error(s"Request '$fullRequestName' failed for user ${session.userId}: ${errorMessage.getOrElse("")}")
      if (!logger.underlying.isTraceEnabled) {
        logger.debug(dump)
      }
    }

    logger.trace(dump)
  }
}
