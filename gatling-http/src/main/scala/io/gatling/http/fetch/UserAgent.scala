/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
package io.gatling.http.fetch

import io.gatling.http.HeaderNames

import org.asynchttpclient.Request

object UserAgent {

  val IE = "MSIE"
  private val MsIeUserAgentRegex = "MSIE ([0-9]+.[0-9]+)".r

  def getAgent(request: Request): Option[UserAgent] =
    Option(request.getHeaders.get(HeaderNames.UserAgent)).flatMap(parseFromHeader)

  def parseFromHeader(userAgent: String): Option[UserAgent] =
    MsIeUserAgentRegex.findFirstMatchIn(userAgent) match {
      case Some(res) => Some(UserAgent(UserAgent.IE, res.group(1).toFloat))
      case None      => None
    }
}

case class UserAgent(name: String, version: Float)
