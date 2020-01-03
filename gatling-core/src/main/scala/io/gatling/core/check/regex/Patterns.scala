/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.core.check.regex

import java.util.regex.Pattern

import io.gatling.core.util.cache.Cache

import com.github.benmanes.caffeine.cache.LoadingCache

class Patterns(cacheMaxCapacity: Long) {

  private val patternCache: LoadingCache[String, Pattern] =
    Cache.newConcurrentLoadingCache(cacheMaxCapacity, Pattern.compile)

  def extractAll[G: GroupExtractor](chars: CharSequence, pattern: String): Seq[G] = {

    val matcher = compilePattern(pattern).matcher(chars)
    matcher
      .foldLeft(List.empty[G]) { (matcher, values) =>
        matcher.value :: values
      }
      .reverse
  }

  def compilePattern(pattern: String): Pattern = patternCache.get(pattern)
}
