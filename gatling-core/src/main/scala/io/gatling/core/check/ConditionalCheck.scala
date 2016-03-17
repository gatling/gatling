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





package io.gatling.core.check

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging

import io.gatling.commons.validation.Validation
import io.gatling.core.Predef.Session
import io.gatling.core.session.Expression

object ConditionalCheck extends LazyLogging {
  
  implicit def conditionalCheckBuilder2Check[R, C <: Check[R]](conditionalCheckBuilder: ConditionalCheckBuilder[R, C]): ConditionalCheck[R, C] = conditionalCheckBuilder.build

  trait ConditionalCheckWrapper[R, C <: Check[R]] {
    def wrap(check: ConditionalCheck[R, C]): C
  }

  def checkIf[R, C <: Check[R]](condition: Expression[Boolean])(thenCheck: C)(implicit cw: ConditionalCheckWrapper[R, C]): C = cw.wrap(ConditionalCheckBuilder((r: R, s: Session) => condition(s), thenCheck, None))
  
  def checkIfOrElse[R, C <: Check[R]](condition: Expression[Boolean])(thenCheck: C)(elseCheck: C)(implicit cw: ConditionalCheckWrapper[R, C]): C = cw.wrap(ConditionalCheckBuilder((r: R, s: Session) => condition(s), thenCheck, Some(elseCheck)))
  
  def checkIf[R, C <: Check[R]](condition: (R, Session) => Validation[Boolean])(thenCheck: C)(implicit cw: ConditionalCheckWrapper[R, C]): C = 
    cw.wrap(ConditionalCheckBuilder((r: R,s: Session) => condition(r, s), thenCheck, None))
        
  def checkIfOrElse[R, C <: Check[R]](condition: (R, Session) => Validation[Boolean])(thenCheck: C)(elseCheck: C)(implicit cw: ConditionalCheckWrapper[R, C]): C = 
    cw.wrap(ConditionalCheckBuilder((r: R,s: Session) => condition(r, s), thenCheck, Some(elseCheck)))

  case class ConditionalCheckBuilder[R, C <: Check[R]](condition: (R, Session) => Validation[Boolean], thenCheck: C, elseCheck: Option[C]) {
    def build = ConditionalCheck[R, C](condition, thenCheck, elseCheck)
  }

  case class ConditionalCheck[R, C <: Check[R]](condition: (R, Session) => Validation[Boolean], thenCheck: C, elseCheck: Option[C]) extends Check[R] {

    val checkUuid: String = UUID.randomUUID.toString
    
    def performNestedCheck(nestedCheck: Check[R], response: R, session: Session)(implicit cache: scala.collection.mutable.Map[Any, Any]): Validation[CheckResult] = {
      nestedCheck.check(response, session)
    }
    
    def check(response: R, session: Session)(implicit cache: scala.collection.mutable.Map[Any, Any]): Validation[CheckResult] = {
      val validationResult = condition(response, session).flatMap { c => 
        if (c) {
          logger.trace("Check: [{}] condition evaluate to true: perform nested then check.", checkUuid)
          performNestedCheck(thenCheck, response, session)
        } else {
          elseCheck match {
          case Some(check) => 
            logger.trace("Check: [{}] condition evaluate to false: perform nested else check.", checkUuid)
            performNestedCheck(elseCheck.get, response, session)
          case None =>
            logger.trace("Check: [{}] condition is false: do not perform any check.", checkUuid)
            CheckResult.NoopCheckResultSuccess
          }
        }
      }

      validationResult
    }

  }
  
}
