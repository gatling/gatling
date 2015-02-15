/**
 * Copyright 2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import io.gatling.core.config.GatlingConfiguration
import io.gatling.http.ahc.{ AhcHttpEngine, HttpEngine }
import io.gatling.http.cache.HttpCaches
import io.gatling.http.config.DefaultHttpProtocol
import io.gatling.http.fetch.ResourceFetcher

trait HttpDefaultComponents {

  def configurationForHttp: GatlingConfiguration

  private implicit def thisConfiguration = configurationForHttp

  implicit lazy val httpEngine: HttpEngine = new AhcHttpEngine

  implicit lazy val httpCaches = new HttpCaches

  implicit lazy val resourceFetcher = new ResourceFetcher

  implicit lazy val defaultHttpProtocol = new DefaultHttpProtocol
}
