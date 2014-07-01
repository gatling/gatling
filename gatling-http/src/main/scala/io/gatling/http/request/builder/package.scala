/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http.request

import scala.annotation.tailrec
import scala.collection.breakOut
import scala.collection.JavaConversions.seqAsJavaList

import com.ning.http.client.FluentStringsMap

import io.gatling.core.session.Session
import io.gatling.core.validation._

package object builder {

  implicit class HttpParams(val params: List[HttpParam]) extends AnyVal {

    def resolveParams(session: Session): Validation[List[(String, String)]] =

      params.foldLeft(HttpParam.EmptyParamListSuccess) { (resolvedParams, param) =>
        val newParams: Validation[List[(String, String)]] = param match {
          case SimpleParam(key, value) =>
            for {
              key <- key(session)
              value <- value(session)
            } yield List(key -> value.toString)

          case MultivaluedParam(key, values) =>
            for {
              key <- key(session)
              values <- values(session)
            } yield values.map(key -> _.toString)(breakOut)

          case ParamSeq(seq) =>
            for {
              seq <- seq(session)
            } yield seq.map { case (key, value) => key -> value.toString }(breakOut)

          case ParamMap(map) =>
            for {
              map <- map(session)
            } yield map.map { case (key, value) => key -> value.toString }(breakOut)
        }

        for {
          newParams <- newParams
          resolvedParams <- resolvedParams
        } yield newParams ::: resolvedParams
      }

    def resolveFluentStringsMap(session: Session): Validation[FluentStringsMap] = {

        def update(fsm: FluentStringsMap, param: HttpParam): Validation[FluentStringsMap] = param match {
          case SimpleParam(key, value) =>
            for {
              key <- key(session)
              value <- value(session)
            } yield fsm.add(key, value.toString)

          case MultivaluedParam(key, values) =>
            for {
              key <- key(session)
              values <- values(session)
            } yield fsm.add(key, values.map(_.toString))

          case ParamSeq(seq) =>
            for {
              seq <- seq(session)
            } yield {
              seq.foreach { case (key, value) => fsm.add(key, value.toString) }
              fsm
            }

          case ParamMap(map) =>
            for {
              map <- map(session)
            } yield {
              map.foreach { case (key, value) => fsm.add(key, value.toString) }
              fsm
            }
        }

        @tailrec
        def resolveFluentStringsMapRec(fsm: FluentStringsMap, currentParams: List[HttpParam]): Validation[FluentStringsMap] =
          currentParams match {
            case Nil => fsm.success
            case head :: tail =>
              update(fsm, head) match {
                case Success(newFsm) => resolveFluentStringsMapRec(newFsm, tail)
                case f               => f
              }
          }

      resolveFluentStringsMapRec(new FluentStringsMap, params)
    }
  }
}
