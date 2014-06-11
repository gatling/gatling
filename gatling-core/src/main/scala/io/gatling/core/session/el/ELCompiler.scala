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
package io.gatling.core.session.el

import java.lang.{ StringBuilder => JStringBuilder }

import scala.collection.breakOut
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.reflect.ClassTag

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.NumberHelper.IntString
import io.gatling.core.util.TypeHelper.TypeCaster
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

import scala.util.parsing.combinator.RegexParsers

object ELMessages {
  def undefinedSeqIndexMessage(name: String, index: Int) = s"Seq named '$name' is undefined for index $index"
  def undefinedSessionAttributeMessage(name: String) = s"No attribute named '$name' is defined"
}

trait Part[+T] {
  def apply(session: Session): Validation[T]
}

case class StaticPart(string: String) extends Part[String] {
  def apply(session: Session): Validation[String] = string.success
}

case class AttributePart(name: String) extends Part[Any] {
  def apply(session: Session): Validation[Any] = session(name).validate[Any]
}

case class SeqSizePart(name: String) extends Part[Int] {
  def apply(session: Session): Validation[Int] = session(name).validate[Seq[_]].map(_.size)
}

case class SeqRandomPart(name: String) extends Part[Any] {
  def apply(session: Session): Validation[Any] = {
      def randomItem(seq: Seq[_]) = seq(ThreadLocalRandom.current.nextInt(seq.size))

    session(name).validate[Seq[_]].map(randomItem)
  }
}

case class SeqElementPart(name: String, index: String) extends Part[Any] {
  def apply(session: Session): Validation[Any] = {

      def seqElementPart(index: Int): Validation[Any] = session(name).validate[Seq[_]].flatMap {
        _.lift(index) match {
          case Some(e) => e.success
          case None    => ELMessages.undefinedSeqIndexMessage(name, index).failure
        }
      }

    index match {
      case IntString(i) => seqElementPart(i)
      case _            => session(index).validate[Int].flatMap(seqElementPart)
    }
  }
}

sealed class ELParserException(message: String) extends Exception(message)
class ELMissingAttributeName(el: String) extends ELParserException(s"An attribute name is missing in this expression : $el")

object ELCompiler {

  def compile[T: ClassTag](string: String): Expression[T] = {
    val elCompiler = new ELCompiler(string)
    val parts = elCompiler.parseEl(string)

    parts match {
      case List(StaticPart(staticStr)) => {
        val stringV = staticStr.asValidation[T]
        _ => stringV
      }

      case List(dynamicPart) => dynamicPart(_).flatMap(_.asValidation[T])

      case _ =>
        (session: Session) => parts.foldLeft(new JStringBuilder(string.length + 5).success) { (sb, part) =>
          part match {
            case StaticPart(string) => sb.map(_.append(string))
            case _ =>
              for {
                sb <- sb
                part <- part(session)
              } yield sb.append(part)
          }
        }.flatMap(_.toString.asValidation[T])
    }
  }
}

class ELCompiler(string: String) extends RegexParsers {

  override def skipWhitespace = false

  def parseEl(string: String): List[Part[Any]] = {
    parseAll(expr, string) match {
      case Success(part, _)    => part
      case Failure(msg, input) => throw new ELParserException(s"Failed to parser ${string} with error ${msg}")
    }
  }

  def expr: Parser[List[Part[Any]]] = (multivaluedExpr | elExpr) ^^ {
    case validation: List[Part[Any]] => validation
    case part: Part[Any]             => List(part)
  }

  def multivaluedExpr: Parser[List[Part[Any]]] = (elExpr | staticPart) *

  def staticPart: Parser[StaticPart] = "[^$]+".r ^^ {
    case staticStr => StaticPart(staticStr)
  }

  def elExpr: Parser[Part[Any]] = "${" ~> (sizeValue | randomValue | seqElement | elValue | emptyExpression) <~ "}"

  def elValue: Parser[Part[Any]] = name ^^ {
    case name => AttributePart(name)
  }

  def sizeValue: Parser[Part[Any]] = name <~ ".size" ^^ {
    case seqName => SeqSizePart(seqName)
  }

  def randomValue: Parser[Part[Any]] = name <~ ".random" ^^ {
    case seqName => SeqRandomPart(seqName)
  }

  def seqElement: Parser[Part[Any]] = name ~ "(" ~ name ~ ")" ^^ {
    case seqName ~ _ ~ posStr ~ _ => SeqElementPart(seqName, posStr)
  }

  def emptyExpression: Parser[Part[Any]] = "" ^^ {
    throw new ELMissingAttributeName(string)
  }

  def name: Parser[String] = "[^.${}()]+".r
}
