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
package io.gatling.http.referer

import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.http.util.HttpHelper.{ isAjax, isHtml }
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.response.Response

import org.asynchttpclient.Request

object RefererHandling {

  val RefererAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.referer"

  def getStoredReferer(session: Session): Option[String] = session(RefererAttributeName).asOption[String]

  def storeReferer(request: Request, response: Response, protocol: HttpProtocol): Session => Session =
    if (protocol.requestPart.autoReferer && !isAjax(request.getHeaders) && isHtml(response.headers))
      _.set(RefererAttributeName, request.getUrl)
    else
      Session.Identity
}
