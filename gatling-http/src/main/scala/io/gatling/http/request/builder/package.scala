/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.http.request

import java.util.{ List => JList, ArrayList => JArrayList }

import scala.annotation.tailrec

import io.gatling.core.session.Session
import io.gatling.core.validation._

import org.asynchttpclient.request.Param

package object builder {

  implicit class HttpParams(val params: List[HttpParam]) extends AnyVal {

    def resolveParamJList(session: Session): Validation[JList[Param]] = {

        def update(ahcParams: JList[Param], param: HttpParam): Validation[JList[Param]] = param match {
          case SimpleParam(key, value) =>
            for {
              key <- key(session)
              value <- value(session)
            } yield {
              ahcParams.add(new Param(key, value.toString))
              ahcParams
            }

          case MultivaluedParam(key, values) =>
            for {
              key <- key(session)
              values <- values(session)
            } yield {
              values.foreach(value => ahcParams.add(new Param(key, value.toString)))
              ahcParams
            }

          case ParamSeq(seq) =>
            for {
              seq <- seq(session)
            } yield {
              seq.foreach { case (key, value) => ahcParams.add(new Param(key, value.toString)) }
              ahcParams
            }

          case ParamMap(map) =>
            for {
              map <- map(session)
            } yield {
              map.foreach { case (key, value) => ahcParams.add(new Param(key, value.toString)) }
              ahcParams
            }
        }

        @tailrec
        def resolveParamJList(ahcParams: JList[Param], currentParams: List[HttpParam]): Validation[JList[Param]] =
          currentParams match {
            case Nil => ahcParams.success
            case head :: tail =>
              update(ahcParams, head) match {
                case Success(newAhcParams) => resolveParamJList(newAhcParams, tail)
                case f                     => f
              }
          }

      resolveParamJList(new JArrayList[Param](params.size), params)
    }
  }
}
