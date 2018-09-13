/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.validation._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.util.Resource
import io.gatling.core.util.cache.Cache

import com.github.benmanes.caffeine.cache.LoadingCache
import com.mitchellbosecke.pebble.template.PebbleTemplate

class PebbleFileBodies(implicit configuration: GatlingConfiguration) {

  private val resourceCache: LoadingCache[String, Validation[Resource]] = {
    val pathToResource: String => Validation[Resource] = path => Resource.resource(path)
    Cache.newConcurrentLoadingCache(configuration.core.pebbleFileBodiesCacheMaxCapacity, pathToResource)
  }

  private val templatesCache: LoadingCache[Resource, Validation[PebbleTemplate]] = {
    val resourceToTemplate: Resource => Validation[PebbleTemplate] = resource =>
      Pebble.parseStringTemplate(resource.string(configuration.core.charset))
    Cache.newConcurrentLoadingCache(configuration.core.pebbleFileBodiesCacheMaxCapacity, resourceToTemplate)
  }

  def asTemplate(filePath: Expression[String]): Expression[PebbleTemplate] =
    filePath match {
      case StaticStringExpression(path) =>
        val template =
          for {
            resource <- resourceCache.get(path)
            template <- templatesCache.get(resource)
          } yield template

        _ => template

      case _ =>
        session =>
          for {
            path <- filePath(session)
            resource <- resourceCache.get(path)
            template <- templatesCache.get(resource)
          } yield template
    }
}
