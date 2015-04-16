/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.body

import java.io.File

import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.config.Resource
import io.gatling.core.session.Expression
import io.gatling.core.util.Io._
import io.gatling.core.util.cache.SelfLoadingThreadSafeCache
import io.gatling.core.validation.Validation

class RawFileBodies(implicit configuration: GatlingConfiguration) {

  private val rawFileBodyCache = {
      def pathToFile(path: String): Validation[File] = Resource.body(path).map(_.file)

    SelfLoadingThreadSafeCache[String, Validation[File]](configuration.core.rawFileBodiesCacheMaxCapacity, pathToFile)
  }

  def asFile(filePath: Expression[String]): Expression[File] = {

    session =>
      for {
        path <- filePath(session)
        file <- rawFileBodyCache.get(path)
        validatedFile <- file.validateExistingReadable
      } yield validatedFile
  }
}
