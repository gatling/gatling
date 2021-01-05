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

import io.gatling.core.session.Session

class SessionCacheHandler[K, V](cacheName: String, maxCapacity: Int) {

  val enabled: Boolean = maxCapacity > 0

  private[cache] def getCache(session: Session): Option[Cache[K, V]] =
    session.attributes.get(cacheName).map(_.asInstanceOf[Cache[K, V]])

  private[cache] def getOrCreateCache(session: Session): Cache[K, V] =
    getCache(session) match {
      case Some(cache) => cache
      case _           => Cache.newImmutableCache[K, V](maxCapacity)
    }

  def addEntry(session: Session, key: K, value: V): Session = {
    val cache = getOrCreateCache(session)
    val newCache = cache.put(key, value)
    if (newCache eq cache) {
      session
    } else {
      session.set(cacheName, newCache)
    }
  }

  def getEntry(session: Session, key: => K): Option[V] =
    getCache(session).flatMap(_.get(key))

  def removeEntry(session: Session, key: K): Session =
    getCache(session) match {
      case Some(cache) =>
        val newCache = cache.remove(key)
        if (newCache eq cache) {
          session
        } else {
          session.set(cacheName, newCache)
        }

      case _ => session
    }
}
