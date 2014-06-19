/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.fetch

import com.ning.http.client.Request

object UserAgent {
  val IE = "MSIE"

  private val USER_AGENT = "User-Agent"
  private val MSIE_AGENT_REGEX = new scala.util.matching.Regex("MSIE ([0-9]+.[0-9]+)")

  def getAgent(request: Request): Option[UserAgent] = {

    if (request.getHeaders.containsKey(USER_AGENT)) {
      val agentStr = request.getHeaders.getFirstValue(USER_AGENT)
      parseFromHeader(agentStr)
    } else
      None
  }

  def parseFromHeader(userAgent: String): Option[UserAgent] = {
    MSIE_AGENT_REGEX.findFirstMatchIn(userAgent) match {
      case Some(res) => Some(UserAgent(UserAgent.IE, res.group(1).toFloat))
      case None      => None
    }
  }
}

case class UserAgent(name: String, version: Float)
