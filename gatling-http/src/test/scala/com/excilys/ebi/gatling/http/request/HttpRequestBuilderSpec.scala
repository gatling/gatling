/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package com.excilys.ebi.gatling.http.request

import java.net.InetAddress

import com.excilys.ebi.gatling.core.session.{NOOP_EVALUATABLE_STRING, Session}
import com.excilys.ebi.gatling.http.config.{HttpProtocolConfiguration, HttpProtocolConfigurationBuilder}
import com.excilys.ebi.gatling.http.request.builder.GetHttpRequestBuilder
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HttpRequestBuilderSpec extends Specification {

  val session = new Session("scenario", 1)
  val protocolBuilder = HttpProtocolConfigurationBuilder.default
    .disableWarmUp
    .baseURL("http://localhost/")
  val config: HttpProtocolConfiguration = protocolBuilder.build

  "http request builder" should {
    "support specifying an address to connect to" in {
      val localhost = InetAddress.getByName("127.0.0.1")

      val builder = GetHttpRequestBuilder(NOOP_EVALUATABLE_STRING, NOOP_EVALUATABLE_STRING)
        .address(localhost)
      val request = builder.build(session, config)

      request.getInetAddress should beEqualTo(localhost)
    }
  }
}
