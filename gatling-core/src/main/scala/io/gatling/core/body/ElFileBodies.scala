/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import java.nio.charset.Charset

import io.gatling.commons.validation._
import io.gatling.core.session._
import io.gatling.core.util.ResourceCache
import io.gatling.core.util.cache.Cache

import com.github.benmanes.caffeine.cache.LoadingCache

final class ElFileBodies(charset: Charset, cacheMaxCapacity: Long) extends ResourceCache {
  private def compileFile(path: String): Validation[List[ElBody.Part]] =
    cachedResource(path).flatMap { resource =>
      safely() {
        val string = resource.string(charset)
        ElBody.toParts(string, charset).success
      }
    }

  private val elFileBodyPartsCache: LoadingCache[String, Validation[List[ElBody.Part]]] =
    Cache.newConcurrentLoadingCache(cacheMaxCapacity, compileFile)

  def parse(filePath: Expression[String]): Expression[List[ElBody.Part]] =
    filePath match {
      case StaticValueExpression(path) =>
        elFileBodyPartsCache.get(path) match {
          case Success(parts) => parts.expressionSuccess // so we return a StaticValueExpression, this might get optimized
          case Failure(error) => error.expressionFailure
        }

      case _ =>
        session =>
          for {
            path <- filePath(session)
            expression <- elFileBodyPartsCache.get(path)
          } yield expression
    }
}
