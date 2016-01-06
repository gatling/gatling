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
package io.gatling.http.request

import java.util.{ ArrayList => JArrayList, Collections => JCollections, List => JList }

import scala.annotation.tailrec
import scala.collection.JavaConversions._

import io.gatling.commons.validation._
import io.gatling.core.session.{ Expression, Session }

import org.asynchttpclient.Param

package object builder {

  val EmptyParamJListSuccess: Validation[JList[Param]] = JCollections.emptyList[Param].success

  implicit class HttpParams(val params: List[HttpParam]) extends AnyVal {

    def mergeWithFormIntoParamJList(formMaybe: Option[Expression[Map[String, Seq[String]]]], session: Session): Validation[JList[Param]] = {

      val formParams = params.resolveParamJList(session)

      formMaybe match {
        case Some(form) =>
          for {
            resolvedFormParams <- formParams
            resolvedForm <- form(session)
          } yield {
            val formParamsByName = resolvedFormParams.groupBy(_.getName)
            val formFieldsByName = resolvedForm.map { case (key, values) => key -> values.map(value => new Param(key, value)) }
            // override form with formParams
            val javaParams: JList[Param] = (formFieldsByName ++ formParamsByName).values.flatten.toSeq
            javaParams
          }

        case None =>
          formParams
      }
    }

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
        def resolveParamJListRec(ahcParams: JList[Param], currentParams: List[HttpParam]): Validation[JList[Param]] =
          currentParams match {
            case Nil => ahcParams.success
            case head :: tail =>
              update(ahcParams, head) match {
                case Success(newAhcParams) => resolveParamJListRec(newAhcParams, tail)
                case f                     => f
              }
          }

      if (params.isEmpty)
        EmptyParamJListSuccess
      else
        resolveParamJListRec(new JArrayList[Param](params.size), params)
    }
  }
}
