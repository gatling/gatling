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

package io.gatling.core.util.cache

import io.gatling.BaseSpec
import io.gatling.core.EmptySession

import org.scalatest.OptionValues

class SessionCacheHandlerSpec extends BaseSpec with OptionValues with EmptySession {

  private val sessionCacheHandler = new SessionCacheHandler[String, String]("stringCache", 1)

  "getCache" should "return None if the cache does not exist" in {
    sessionCacheHandler.getCache(emptySession) shouldBe empty
  }

  it should "return the cache if it exists" in {
    val newCache = Cache.newImmutableCache[String, String](2)
    val sessionWithCache = emptySession.set("stringCache", newCache)
    sessionCacheHandler.getCache(sessionWithCache) should not be empty
    sessionCacheHandler.getCache(sessionWithCache).value should be theSameInstanceAs newCache
  }

  "getOrCreateCache" should "return the cache if it exists" in {
    val newCache = Cache.newImmutableCache[String, String](2)
    val sessionWithCache = emptySession.set("stringCache", newCache)
    sessionCacheHandler.getOrCreateCache(sessionWithCache) should be theSameInstanceAs newCache
  }

  it should "create a new cache if it didn't exists" in {
    emptySession.contains("stringCache") shouldBe false
    sessionCacheHandler.getOrCreateCache(emptySession) shouldBe a[Cache[_, _]] // TODO : Can this test be improved ?
  }

  "addEntry" should "add a new entry to the cache" in {
    val sessionWithNewEntry = sessionCacheHandler.addEntry(emptySession, "foo", "bar")
    val entry = sessionCacheHandler.getOrCreateCache(sessionWithNewEntry).get("foo")

    entry should not be empty
    entry.value shouldBe "bar"
  }

  "getEntry" should "return None if the cache does not exists" in {
    sessionCacheHandler.getEntry(emptySession, "foo") shouldBe empty
  }

  it should "return None if the entry does not exists" in {
    val sessionWithCache = sessionCacheHandler.addEntry(emptySession, "foo", "bar")
    sessionCacheHandler.getEntry(sessionWithCache, "quz") shouldBe empty
  }

  it should "return the value if the cache and entry exists" in {
    val sessionWithCache = sessionCacheHandler.addEntry(emptySession, "foo", "bar")
    val entry = sessionCacheHandler.getEntry(sessionWithCache, "foo")

    entry should not be empty
    entry.value shouldBe "bar"
  }

  "removeEntry" should "left the session untouched if the cache doesn't exist" in {
    sessionCacheHandler.removeEntry(emptySession, "foo") should be theSameInstanceAs emptySession
  }

  it should "remove the key from the cache if it exists" in {
    val sessionWithEntry = sessionCacheHandler.addEntry(emptySession, "foo", "bar")
    val sessionWithoutEntry = sessionCacheHandler.removeEntry(sessionWithEntry, "foo")

    sessionCacheHandler.getEntry(sessionWithoutEntry, "foo") shouldBe empty
  }
}
