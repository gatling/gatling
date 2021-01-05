/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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
import io.gatling.http.client.Request
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.response.Response
import io.gatling.http.util.HttpHelper.{ isAjax, isHtml }

private[http] object RefererHandling {

  private val RefererAttributeName = SessionPrivateAttributes.PrivateAttributePrefix + "http.referer"

  def getStoredReferer(session: Session): Option[String] = session(RefererAttributeName).asOption[String]

  def storeReferer(request: Request, response: Response, protocol: HttpProtocol): Session => Session =
    if (protocol.requestPart.autoReferer && isHtml(response.headers) && !isAjax(request.getHeaders))
      _.set(RefererAttributeName, request.getUri.toUrl)
    else
      Session.Identity
}
