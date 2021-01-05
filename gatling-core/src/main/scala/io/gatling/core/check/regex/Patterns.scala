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

package io.gatling.core.check.regex

import java.util.regex.Pattern

import scala.annotation.tailrec

import io.gatling.core.util.cache.Cache

import com.github.benmanes.caffeine.cache.LoadingCache

class Patterns(cacheMaxCapacity: Long) {

  private val patternCache: LoadingCache[String, Pattern] =
    Cache.newConcurrentLoadingCache(cacheMaxCapacity, Pattern.compile)

  private def compilePattern(pattern: String): Pattern = patternCache.get(pattern)

  def find[X: GroupExtractor](string: String, pattern: String, n: Int): Option[X] = {

    val matcher = compilePattern(pattern).matcher(string)

    @tailrec
    def findRec(countDown: Int): Boolean = matcher.find && (countDown == 0 || findRec(countDown - 1))

    if (findRec(n))
      Some(GroupExtractor[X].extract(matcher))
    else
      None
  }

  def findAll[X: GroupExtractor](string: String, pattern: String): Seq[X] = {

    val matcher = compilePattern(pattern).matcher(string)

    var acc = List.empty[X]
    while (matcher.find) {
      acc = GroupExtractor[X].extract(matcher) :: acc
    }
    acc.reverse
  }

  def count(string: String, pattern: String): Int = {
    val matcher = compilePattern(pattern).matcher(string)

    var count = 0
    while (matcher.find) count = count + 1
    count
  }
}
