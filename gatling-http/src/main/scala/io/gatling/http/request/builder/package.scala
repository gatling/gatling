/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.validation._
import io.gatling.core.session.Session
import io.gatling.http.client.Param

package object builder {
  private val EmptyParamJListSuccess: Validation[ju.List[Param]] = ju.Collections.emptyList[Param].success

  def resolveParamJList(params: List[HttpParam], session: Session): Validation[ju.List[Param]] = {
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
