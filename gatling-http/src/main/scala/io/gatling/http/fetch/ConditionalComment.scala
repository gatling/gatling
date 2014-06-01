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
package io.gatling.http.fetch

import scala.util.parsing.combinator._
import com.typesafe.scalalogging.slf4j.StrictLogging

object ConditionalComment {
  val IE = "IE"
  // Conditional comments support was dropped in IE 10
  val LAST_CC_VERSION = 10
}

class CCParseException(msg: String) extends Exception(msg)

class ConditionalComment(browser: Option[Browser]) extends JavaTokenParsers with StrictLogging {

  def evaluate(condition: CharSequence): Boolean = {
    browser match {
      case Some(Browser(browserName, clientVersion)) => browserName match {
        case ConditionalComment.IE =>
          if (clientVersion >= ConditionalComment.LAST_CC_VERSION) false
          else {
            parseAll(comment, condition) match {
              case Success(res, _)     => res
              case Failure(msg, input) => throw new CCParseException(msg)
            }
          }

        case _ => false
      }
      // Only IE supports conditional comments
      case _ => false
    }
  }

  def comment: Parser[Boolean] = "if" ~> expression

  def expression: Parser[Boolean] = (notToken ?) ~ condition ^^ {
    case nToken ~ value =>
      nToken match {
        case Some("!") => !value
        case _         => value
      }
  }

  def condition: Parser[Boolean] = andCondition | simpleCondition

  def andCondition: Parser[Boolean] = orCondition ~ (("&" ~> orCondition) ?) ^^ {
    case v1 ~ opV2 => opV2 match {
      case Some(v2) => v1 && v2
      case None     => v1
    }
  }

  def orCondition: Parser[Boolean] = orExpr ~ (("|" ~> orExpr) ?) ^^ {
    case v1 ~ opV2 => opV2 match {
      case Some(v2) => v1 || v2
      case None     => v1
    }
  }

  def orExpr: Parser[Boolean] = parenthesesCondition | trueToken | falseToken

  def parenthesesCondition: Parser[Boolean] = "(" ~> condition <~ ")"

  def simpleCondition: Parser[Boolean] = operatorExpression | trueToken | falseToken

  def operatorExpression: Parser[Boolean] = (operator ?) ~ "IE" ~ number ^^ {
    case op ~ _ ~ num => {
      val (version, numVal) = num match {
        case Left(n)  => (browser.get.version, n.toDouble)
        case Right(n) => (browser.get.version.floor, n.toDouble)
      }

      op match {
        case Some("lt")  => version < numVal
        case Some("gt")  => version > numVal
        case Some("gte") => version >= numVal
        case Some("lte") => version <= numVal
        case None        => version == numVal
      }
    }
  }

  def operator: Parser[String] = "gte" | "lte" | "gt" | "lt"

  def number: Parser[Either[Double, Int]] = (doubleNumber | intNumber) ^^ {
    case dNum if dNum.isInstanceOf[Double] => Left(dNum.asInstanceOf[Double])
    case iNum if iNum.isInstanceOf[Int]    => Right(iNum.asInstanceOf[Int])
  }

  def intNumber: Parser[Int] = "[0-9]+".r ^^ {
    case numStr => numStr.toInt
  }

  def doubleNumber: Parser[Double] = "[0-9]+(.[0-9]+)".r ^^ {
    case numStr => numStr.toDouble
  }

  def notToken: Parser[String] = "!"

  def trueToken: Parser[Boolean] = "true".r ^^ { case _ => true }

  def falseToken: Parser[Boolean] = "false".r ^^ { case _ => false }
}

