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
package io.gatling.http.util

import java.util.UUID

import scala.util.Properties._

import com.ning.http.client.{ AsyncHttpClientConfig, RequestBuilder, AsyncHttpClient }
import io.gatling.core.config.GatlingConfiguration._
import io.gatling.core.util.IO._
import io.gatling.http.HeaderNames._

object GA {

  def send(): Unit =
    if (configuration.http.enableGA) {
      val trackingId = if (configuration.core.version.endsWith("SNAPSHOT")) "UA-53375088-4" else "UA-53375088-5"
      val request = new RequestBuilder("POST")
        .setUrl("https://ssl.google-analytics.com/collect")
        .addFormParam("tid", trackingId)
        .addFormParam("dl", s"http://gatling.io/${configuration.core.version}")
        .addFormParam("de", "UTF-8")
        .addFormParam("ul", "en-US")
        .addFormParam("t", "pageview")
        .addFormParam("v", "1")
        .addFormParam("dt", configuration.core.version)
        .addFormParam("cid", UUID.randomUUID.toString)
        .setHeader(Connection, "Keep-Alive")
        .setHeader(UserAgent, s"java/$javaVersion")
        .setRequestTimeout(2000)
        .build

      val config = new AsyncHttpClientConfig.Builder().setMaxRequestRetry(0).build

      withCloseable(new AsyncHttpClient(config)) { _.executeRequest(request).get }
    }
}
