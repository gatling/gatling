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
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation, Success, Failure }

import scala.util.parsing.combinator.RegexParsers

object ELMessages {
  def undefinedSeqIndexMessage(name: String, index: Int) = s"Seq named '$name' is undefined for index $index"
  def undefinedSessionAttributeMessage(name: String) = s"No attribute named '$name' is defined"
  def undefinedMapKeyMessage(map: String, key: String) = s"Map named '$map' does not contain key '$key'"
  def incorrectTypeMessage(typeName: String) = s"Unexpected type '$typeName'"
  def sizeNotSupportedMessage(value: Any, name: String) = s"${value} named '${name}' does not support .size function"
  def accessByKeyNotSupportedMessage(value: Any, name: String) = s"${value} named '${name}' does not support access by key"
  def randomNotSupportedMessage(value: Any, name: String) = s"${value} named '${name}' does not support .random function"
  def indexAccessNotSupportedMessage(value: Any, name: String) = s"${value} named '${name}' does not support index access"
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

case class SizePart(seqPart: Part[Any], name: String) extends Part[Int] {
  def apply(session: Session): Validation[Int] = {
    seqPart(session) match {
      case Success(t: Traversable[_])                    => t.size.success
      case Success(jcollection: java.util.Collection[_]) => jcollection.size.success
      case Success(jmap: java.util.Map[_, _])            => jmap.size.success
      case Success(arr: Array[_])                        => arr.length.success
      case Success(other)                                => ELMessages.sizeNotSupportedMessage(other, name).failure
      case f: Failure                                    => f
    }
  }
}

case class RandomPart(seq: Part[Any], name: String) extends Part[Any] {
  def apply(session: Session): Validation[Any] = {
      def random(size: Int) = ThreadLocalRandom.current.nextInt(size)

    seq(session) match {
      case Success(s: Seq[_])                => s(random(s.size)).success
      case Success(jlist: java.util.List[_]) => jlist.get(random(jlist.size)).success
      case Success(arr: Array[_])            => arr(random(arr.length)).success
      case Success(other)                    => ELMessages.randomNotSupportedMessage(other, name).failure
      case f: Failure                        => f
    }
  }
}

case class SeqElementPart(seq: Part[Any], seqName: String, index: String) extends Part[Any] {
  def apply(session: Session): Validation[Any] = {

      def seqElementPart(index: Int): Validation[Any] = seq(session) match {
        case Success(s: Seq[_]) =>
          s.lift(index) match {
            case Some(e) => e.success
            case None    => ELMessages.undefinedSeqIndexMessage(seqName, index).failure
          }

        case Success(arr: Array[_]) =>
          if (index < arr.length) arr(index).success
          else ELMessages.undefinedSeqIndexMessage(seqName, index).failure

        case Success(jlist: java.util.List[_]) =>
          if (index < jlist.size()) jlist.get(index).success
          else ELMessages.undefinedSeqIndexMessage(seqName, index).failure

        case Success(other) => ELMessages.indexAccessNotSupportedMessage(other, seqName).failure

        case f: Failure     => f
      }

    index match {
      case IntString(i) => seqElementPart(i)
      case _            => session(index).validate[Int].flatMap(seqElementPart)
    }
  }
}

case class MapKeyPart(map: Part[Any], mapName: String, key: String) extends Part[Any] {

  def apply(session: Session): Validation[Any] = {
    map(session) match {
      case Success(m: Map[Any, Any]) => m.get(key) match {
        case Some(value) => value.success
        case None        => ELMessages.undefinedMapKeyMessage(mapName, key).failure
      }

      case Success(jmap: java.util.Map[Any, Any]) =>
        if (jmap.containsKey(key)) jmap.get(key).success
        else ELMessages.undefinedMapKeyMessage(mapName, key).failure

      case Success(other) => ELMessages.accessByKeyNotSupportedMessage(other, mapName).failure

      case f: Failure     => f
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

  abstract class AccessToken(val token: String)
  case class AccessIndex(pos: String, override val token: String) extends AccessToken(token)
  case class AccessKey(key: String, override val token: String) extends AccessToken(token)
  case class AccessRandom() extends AccessToken(".random")
  case class AccessSize() extends AccessToken(".size")

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

  def elExpr: Parser[Part[Any]] = "${" ~> (sessionObject | emptyExpression) <~ "}"

  def sessionObject: Parser[Part[Any]] = objectName ~ (((valueAccess) +) ?) ^^ {
    case objectPart ~ accessTokens => {
      val parts = accessTokens match {
        case Some(lst) => lst
        case None      => Nil
      }

      val part = parts.foldLeft(objectPart.asInstanceOf[Part[Any]] -> objectPart.name)((partName, token) =>
        (token match {
          case AccessIndex(pos, tokenName) => SeqElementPart(partName._1, partName._2, pos)
          case AccessKey(key, tokenName)   => MapKeyPart(partName._1, partName._2, key)
          case AccessRandom()              => RandomPart(partName._1, partName._2)
          case AccessSize()                => SizePart(partName._1, partName._2)
        }) -> (partName._2 + token.token))
      part._1
    }
  }

  def objectName: Parser[AttributePart] = name ^^ {
    case name => AttributePart(name)
  }

  def valueAccess: Parser[AccessToken] = (indexAccess | randomAccess | sizeAccess | keyAccess)

  def randomAccess: Parser[AccessToken] = ".random" ^^ {
    case _ => AccessRandom()
  }

  def sizeAccess: Parser[AccessToken] = ".size" ^^ {
    case _ => AccessSize()
  }

  def indexAccess: Parser[AccessToken] = "(" ~> name <~ ")" ^^ {
    case posStr => AccessIndex(posStr, s"(${posStr}})")
  }

  def keyAccess: Parser[AccessToken] = "." ~> name ^^ {
    case keyName => AccessKey(keyName, "." + keyName)
  }

  def emptyExpression: Parser[Part[Any]] = "" ^^ {
    throw new ELMissingAttributeName(string)
  }

  def name: Parser[String] = "[^.${}()]+".r
}
