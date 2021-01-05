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

package io.gatling.http.feeder

import io.gatling.commons.validation._
import io.gatling.core.config.{ GatlingConfiguration, GatlingFiles }
import io.gatling.core.feeder.{ InMemoryFeederSource, SourceFeederBuilder }
import io.gatling.core.util.{ Resource, ResourceCache }

/**
 * Feeder for [[http://www.sitemaps.org/protocol.html sitemap]] file format.
 */
trait SitemapFeederSupport extends ResourceCache {

  def sitemap(fileName: String)(implicit configuration: GatlingConfiguration): SourceFeederBuilder[String] =
    sitemap(cachedResource(GatlingFiles.resourcesDirectory(configuration), fileName))

  def sitemap(resource: Validation[Resource])(implicit configuration: GatlingConfiguration): SourceFeederBuilder[String] =
    resource match {
      case Success(res)     => SourceFeederBuilder(InMemoryFeederSource(SitemapParser.parse(res, configuration.core.charset)), configuration)
      case Failure(message) => throw new IllegalArgumentException(s"Could not locate sitemap file: $message")
    }
}
