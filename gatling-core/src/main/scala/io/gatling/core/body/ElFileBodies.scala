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

import io.gatling.commons.util.Io._
import io.gatling.commons.validation._
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.session._
import io.gatling.core.session.el.{ El, ElCompiler }
import io.gatling.core.util.Resource
import io.gatling.core.util.cache.Cache

import com.github.benmanes.caffeine.cache.LoadingCache

class ElFileBodies(implicit configuration: GatlingConfiguration) {

  private val charset = configuration.core.charset

  private def compileFile(path: String): Validation[Expression[String]] =
    Resource.resource(path).map { resource =>
      withCloseable(resource.inputStream) {
        _.toString(charset)
      }
    }.map(_.el[String])

  private val elFileBodyStringCache: LoadingCache[String, Validation[Expression[String]]] =
    Cache.newConcurrentLoadingCache(configuration.core.elFileBodiesCacheMaxCapacity, compileFile)

  private def resource2BytesSeq(path: String): Validation[Expression[Seq[Array[Byte]]]] =
    Resource.resource(path).map { resource =>
      ElCompiler.compile2BytesSeq(resource.string(charset), charset)
    }

  private val elFileBodyBytesCache: LoadingCache[String, Validation[Expression[Seq[Array[Byte]]]]] =
    Cache.newConcurrentLoadingCache(configuration.core.elFileBodiesCacheMaxCapacity, resource2BytesSeq)

  def asString(filePath: Expression[String]): Expression[String] =
    filePath match {
      case StaticStringExpression(path) =>
        elFileBodyStringCache.get(path) match {
          case Success(expression) => expression
          case Failure(error)      => error.expressionFailure
        }

      case _ =>
        session =>
          for {
            path <- filePath(session)
            expression <- elFileBodyStringCache.get(path)
            body <- expression(session)
          } yield body
    }

  def asBytesSeq(filePath: Expression[String]): Expression[Seq[Array[Byte]]] =
    filePath match {
      case StaticStringExpression(path) =>
        elFileBodyBytesCache.get(path) match {
          case Success(expression) => expression
          case Failure(error)      => error.expressionFailure
        }

      case _ =>
        session =>
          for {
            path <- filePath(session)
            expression <- elFileBodyBytesCache.get(path)
            body <- expression(session)
          } yield body
    }
}
