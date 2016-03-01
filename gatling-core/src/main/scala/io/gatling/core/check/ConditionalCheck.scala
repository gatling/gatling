package io.gatling.core.check

import com.typesafe.scalalogging.LazyLogging

import io.gatling.core.session._
import io.gatling.commons.validation.Validation
import io.gatling.commons.validation.Success
import io.gatling.core.check.extractor.Extractor
import java.util.UUID
import io.gatling.core.check.CheckResult
import io.gatling.core.check.Check
import io.gatling.core.check.DefaultFindCheckBuilder
import io.gatling.core.check.{ Check, Extender, Preparer }
import io.gatling.core.Predef._

object ConditionalCheck extends LazyLogging {
  
  trait CheckWrapper[R, C <: Check[R]] {
    def wrap(check: Check[R]): C
  }

  def checkIf[R, C <: Check[R]](condition: Expression[Boolean])(thenCheck: Check[R])(implicit cw: CheckWrapper[R, C]): C = cw.wrap(new ConditionalCheckBuilder((r: R, s: Session) => condition(s), thenCheck, None))
  
  def checkIfOrElse[R, C <: Check[R]](condition: Expression[Boolean])(thenCheck: Check[R])(elseCheck: Check[R])(implicit cw: CheckWrapper[R, C]): C = cw.wrap(new ConditionalCheckBuilder((r: R, s: Session) => condition(s), thenCheck, Some(elseCheck)))
  
  def checkIf[R, C <: Check[R]](condition: (R, Session) => Validation[Boolean])(thenCheck: Check[R])(implicit cw: CheckWrapper[R, C]): C = 
    cw.wrap(new ConditionalCheckBuilder(
        (r: R,s: Session) => condition(r, s)
        , thenCheck
        , None))
        
  def checkIfOrElse[R, C <: Check[R]](condition: (R, Session) => Validation[Boolean])(thenCheck: Check[R])(elseCheck: Check[R])(implicit cw: CheckWrapper[R, C]): C = 
    cw.wrap(new ConditionalCheckBuilder(
        (r: R,s: Session) => condition(r, s)
        , thenCheck
        , Some(elseCheck)))

  class DummyExtractor[R] extends Extractor[R, R] {
    def name = "dummy"
    def arity = "dummy"
    def apply(prepared: R): Validation[Option[R]] = { Success(Some(prepared)) }
  }

  def dummyPreparer[R]: Preparer[R, R] = (r: R) => Success(r)
  def dummyExtractor[R]: Expression[Extractor[R, R]] = new ExpressionSuccessWrapper(new DummyExtractor[R]()).expressionSuccess

  class ConditionalCheckBuilder[R, C <: Check[R]](condition: (R, Session) => Validation[Boolean], thenCheck: C, elseCheck: Option[C]) 
      extends DefaultFindCheckBuilder[Check[R], R, R, R](
          (wrapped: Check[R]) => new ConditionalCheck[R, C](wrapped, condition, thenCheck, elseCheck), dummyPreparer, dummyExtractor) {

  }
  
  class ConditionalCheck[R, C <: Check[R]](wrapped: Check[R], condition: (R, Session) => Validation[Boolean], thenCheck: C, elseCheck: Option[C])
    extends Check[R] {

    val checkUuid: String = UUID.randomUUID.toString
    
    def performNestedCheck(nestedCheck: Check[R], response: R, session: Session)(implicit cache: scala.collection.mutable.Map[Any, Any]): Validation[CheckResult] = {
      nestedCheck.check(response, session)
    }
    
    def check(response: R, session: Session)(implicit cache: scala.collection.mutable.Map[Any, Any]): Validation[CheckResult] = {
      val validationResult = condition.apply(response, session).flatMap { c => {
        if (c) {
          logger.trace("Check: [{}] condition evaluate to true: perform nested then check.", checkUuid)
          performNestedCheck(thenCheck, response, session)
        } else {
          if (elseCheck.isDefined) {
            logger.trace("Check: [{}] condition evaluate to false: perform nested else check.", checkUuid)
            performNestedCheck(elseCheck.get, response, session)
          } else {
            logger.trace("Check: [{}] condition is false: do not perform any check.", checkUuid)
            CheckResult.NoopCheckResultSuccess
          }
        }} 
      }

      validationResult
    }
  }
  
}