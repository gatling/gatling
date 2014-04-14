/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.feeder

import scala.reflect.io.File
import io.gatling.core.feeder.RecordSeqFeederBuilder
import io.gatling.core.config.Resource
import io.gatling.core.validation.{ Failure, Success, Validation }

/**
 * Feeder for [[http://www.sitemaps.org/protocol.html sitemap]] file format.
 *
 * @author Ivan Mushketyk
 */
trait SitemapFeederSupport {

  def sitemap(file: File): RecordSeqFeederBuilder[String] = sitemap(file.path)
  def sitemap(fileName: String): RecordSeqFeederBuilder[String] = sitemap(Resource.feeder(fileName))

  def sitemap(resource: Validation[Resource]): RecordSeqFeederBuilder[String] = {
    resource match {
      case Success(res)     => RecordSeqFeederBuilder(SitemapParser.parse(res))
      case Failure(message) => throw new IllegalArgumentException(s"Could not locate sitemap file; $message")
    }
  }
}
