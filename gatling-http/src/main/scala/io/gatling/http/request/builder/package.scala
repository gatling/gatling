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

package io.gatling.http.request

import java.{ util => ju }

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

import io.gatling.commons.validation._
import io.gatling.core.session.{ Expression, Session }
import io.gatling.http.client.Param

package object builder {

  val EmptyParamJListSuccess: Validation[ju.List[Param]] = ju.Collections.emptyList[Param].success

  implicit class HttpParams(val params: List[HttpParam]) extends AnyVal {

    def mergeWithFormIntoParamJList(formMaybe: Option[Expression[Map[String, Any]]], session: Session): Validation[ju.List[Param]] = {

      val formParams = params.resolveParamJList(session)

      formMaybe match {
        case Some(form) =>
          for {
            resolvedFormParams <- formParams
            resolvedForm <- form(session)
          } yield {
            val formParamsByName = resolvedFormParams.asScala.groupBy(_.getName)
            val formFieldsByName: Map[String, Seq[Param]] =
              resolvedForm.map { case (key, value) =>
                value match {
                  case multipleValues: Seq[_] => key -> multipleValues.map(value => new Param(key, value.toString))
                  case monoValue              => key -> Seq(new Param(key, monoValue.toString))
                }
              }
            // override form with formParams
            val javaParams: ju.List[Param] = (formFieldsByName ++ formParamsByName).values.flatten.toSeq.asJava
            javaParams
          }

        case _ =>
          formParams
      }
    }

    def resolveParamJList(session: Session): Validation[ju.List[Param]] = {

      def update(clientParams: ju.List[Param], param: HttpParam): Validation[ju.List[Param]] = param match {
        case SimpleParam(key, value) =>
          for {
            key <- key(session)
            value <- value(session)
          } yield {
            clientParams.add(new Param(key, value.toString))
            clientParams
          }

        case MultivaluedParam(key, values) =>
          for {
            key <- key(session)
            values <- values(session)
          } yield {
            values.foreach(value => clientParams.add(new Param(key, value.toString)))
            clientParams
          }

        case ParamSeq(seq) =>
          for {
            seq <- seq(session)
          } yield {
            seq.foreach { case (key, value) => clientParams.add(new Param(key, value.toString)) }
            clientParams
          }

        case ParamMap(map) =>
          for {
            map <- map(session)
          } yield {
            map.foreach { case (key, value) => clientParams.add(new Param(key, value.toString)) }
            clientParams
          }
      }

      @tailrec
      def resolveParamJListRec(clientParams: ju.List[Param], currentParams: List[HttpParam]): Validation[ju.List[Param]] =
        currentParams match {
          case Nil => clientParams.success
          case head :: tail =>
            update(clientParams, head) match {
              case Success(newClientParams) => resolveParamJListRec(newClientParams, tail)
              case f                        => f
            }
        }

      if (params.isEmpty)
        EmptyParamJListSuccess
      else
        resolveParamJListRec(new ju.ArrayList[Param](params.size), params)
    }
  }
}
