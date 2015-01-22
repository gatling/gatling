/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.Resource
import io.gatling.core.session._
import io.gatling.core.session.el.{ ElCompiler, El }
import io.gatling.core.util.Io._
import io.gatling.core.util.cache._
import io.gatling.core.validation._

object ELFileBodies {

  val ELFileBodyStringCache = ThreadSafeCache[String, Validation[Expression[String]]](configuration.http.elFileBodiesCacheMaxCapacity)

  def asString(filePath: Expression[String]): Expression[String] = {

      def compileFile(path: String): Validation[Expression[String]] =
        Resource.body(path)
          .map(resource => withCloseable(resource.inputStream) {
            _.toString(configuration.core.charset)
          }).map(_.el[String])

      def pathToExpression(path: String) =
        if (ELFileBodyStringCache.enabled)
          ELFileBodyStringCache.getOrElsePutIfAbsent(path, compileFile(path))
        else
          compileFile(path)

    session =>
      for {
        path <- filePath(session)
        expression <- pathToExpression(path)
        body <- expression(session)
      } yield body
  }

  val ELFileBodyBytesCache = ThreadSafeCache[String, Validation[Expression[Seq[Array[Byte]]]]](configuration.http.elFileBodiesCacheMaxCapacity)

  def asBytesSeq(filePath: Expression[String]): Expression[Seq[Array[Byte]]] = {

      def resource2BytesSeq(path: String): Validation[Expression[Seq[Array[Byte]]]] = Resource.body(path).map { resource =>
        ElCompiler.compile2BytesSeq(resource.string(configuration.core.charset))
      }

      def pathToExpression(path: String) =
        if (ELFileBodyBytesCache.enabled) ELFileBodyBytesCache.getOrElsePutIfAbsent(path, resource2BytesSeq(path))
        else resource2BytesSeq(path)

    session =>
      for {
        path <- filePath(session)
        expression <- pathToExpression(path)
        body <- expression(session)
      } yield body
  }
}
