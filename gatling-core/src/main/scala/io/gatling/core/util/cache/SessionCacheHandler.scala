/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.util.TypeCaster
import io.gatling.commons.validation._
import io.gatling.core.session.Session

object SessionCacheHandler {

  private[this] val CacheTypeCaster = new TypeCaster[Cache[_, _]] {
    @throws[ClassCastException]
    override def cast(value: Any): Cache[_, _] =
      value match {
        case v: Cache[_, _] => v
        case _              => throw new ClassCastException(cceMessage(value, classOf[Cache[_, _]]))
      }

    override def validate(value: Any): Validation[Cache[_, _]] =
      value match {
        case v: Cache[_, _] => v.success
        case _              => cceMessage(value, classOf[Cache[_, _]]).failure
      }
  }

  implicit def cacheTypeCaster[K, V]: TypeCaster[Cache[K, V]] = CacheTypeCaster.asInstanceOf[TypeCaster[Cache[K, V]]]
}

class SessionCacheHandler[K, V](cacheName: String, maxCapacity: Int) {

  import SessionCacheHandler._

  def getCache(session: Session): Option[Cache[K, V]] =
    session(cacheName).asOption[Cache[K, V]]

  def getOrCreateCache(session: Session): Cache[K, V] =
    getCache(session) match {
      case Some(cache) => cache
      case _           => Cache.newImmutableCache[K, V](maxCapacity)
    }

  def addEntry(session: Session, key: K, value: V): Session = {
    val cache = getOrCreateCache(session)
    cache.get(key) match {
      case Some(`value`) => session
      case _             => session.set(cacheName, cache + (key -> value))
    }
  }

  def getEntry(session: Session, key: => K): Option[V] =
    getCache(session).flatMap(_.get(key))

  def removeEntry(session: Session, key: K): Session =
    getCache(session) match {
      case Some(store) => session.set(cacheName, store - key)
      case _           => session
    }
}
