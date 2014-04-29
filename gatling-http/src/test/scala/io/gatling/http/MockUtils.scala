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
package io.gatling.http

import org.specs2.mock.Mockito

import io.gatling.core.session.Session
import io.gatling.http.config.{ HttpProtocolRequestPart, HttpProtocol }
import io.gatling.http.ahc.HttpTx

import java.net.URI

import com.ning.http.client.Request

/**
 * @author Ivan Mushketyk
 */
object MockUtils extends Mockito {

  def txTo(uri: String, session: Session, redirectCount: Int = 0) = {
    val protocol = mock[HttpProtocol]
    val request = mock[Request]
    val requestPart = mock[HttpProtocolRequestPart]

    val tx = HttpTx(session,
      request,
      "mockHttpTx",
      List(),
      null,
      protocol,
      null,
      true,
      Some(10),
      false,
      false,
      Seq(),
      None,
      false,
      redirectCount)

    protocol.requestPart returns requestPart
    requestPart.cache returns false

    request.getURI returns (new URI(uri))

    tx
  }
}
