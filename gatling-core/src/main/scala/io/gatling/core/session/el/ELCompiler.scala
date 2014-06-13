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

import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.reflect.ClassTag

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.NumberHelper.IntString
import io.gatling.core.util.TypeHelper.TypeCaster
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation, Success, Failure }

import scala.util.parsing.combinator.RegexParsers

object ELMessages {
  def undefinedSeqIndex(name: String, index: Int) = s"Seq named '$name' is undefined for index $index".failure
  def undefinedSessionAttribute(name: String) = s"No attribute named '$name' is defined".failure
  def undefinedMapKey(map: String, key: String) = s"Map named '$map' does not contain key '$key'".failure
  def sizeNotSupported(value: Any, name: String) = s"$value named '$name' does not support .size function".failure
  def accessByKeyNotSupported(value: Any, name: String) = s"$value named '$name' does not support access by key".failure
  def randomNotSupported(value: Any, name: String) = s"$value named '$name' does not support .random function".failure
  def indexAccessNotSupported(value: Any, name: String) = s"$value named '$name' does not support index access".failure
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
  def apply(session: Session): Validation[Int] =
    seqPart(session) match {
      case Success(t: Traversable[_])                   => t.size.success
      case Success(collection: java.util.Collection[_]) => collection.size.success
      case Success(map: java.util.Map[_, _])            => map.size.success
      case Success(arr: Array[_])                       => arr.length.success
      case Success(other)                               => ELMessages.sizeNotSupported(other, name)
      case f: Failure                                   => f
    }
}

case class RandomPart(seq: Part[Any], name: String) extends Part[Any] {
  def apply(session: Session): Validation[Any] = {
      def random(size: Int) = ThreadLocalRandom.current.nextInt(size)

    seq(session) match {
      case Success(s: Seq[_])               => s(random(s.size)).success
      case Success(list: java.util.List[_]) => list.get(random(list.size)).success
      case Success(arr: Array[_])           => arr(random(arr.length)).success
      case Success(other)                   => ELMessages.randomNotSupported(other, name)
      case f: Failure                       => f
    }
  }
}

case class SeqElementPart(seq: Part[Any], seqName: String, index: String) extends Part[Any] {
  def apply(session: Session): Validation[Any] = {

      def seqElementPart(index: Int): Validation[Any] = seq(session) match {
        case Success(s: Seq[_]) =>
          s.lift(index) match {
            case Some(e) => e.success
            case None    => ELMessages.undefinedSeqIndex(seqName, index)
          }

        case Success(arr: Array[_]) =>
          if (index < arr.length) arr(index).success
          else ELMessages.undefinedSeqIndex(seqName, index)

        case Success(list: java.util.List[_]) =>
          if (index < list.size) list.get(index).success
          else ELMessages.undefinedSeqIndex(seqName, index)

        case Success(other) => ELMessages.indexAccessNotSupported(other, seqName)

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
        case None        => ELMessages.undefinedMapKey(mapName, key)
      }

      case Success(map: java.util.Map[Any, Any]) =>
        if (map.containsKey(key)) map.get(key).success
        else ELMessages.undefinedMapKey(mapName, key)

      case Success(other) => ELMessages.accessByKeyNotSupported(other, mapName)

      case f: Failure     => f
    }
  }
}

sealed class ELParserException(message: String) extends Exception(message)
class ELMissingAttributeName(el: String) extends ELParserException(s"An attribute name is missing in expression: $el")

object ELCompiler {

  val StaticPartPattern = "[^$]+".r
  val NamePattern = "[^.${}()]+".r

  val ElCompiler = new ThreadLocal[ELCompiler] { override def initialValue = new ELCompiler }

  def compile[T: ClassTag](string: String): Expression[T] = {
    val parts = ElCompiler.get.parseEl(string)

    parts match {
      case List(StaticPart(staticStr)) =>
        val stringV = staticStr.asValidation[T]
        _ => stringV

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

class ELCompiler extends RegexParsers {

  import ELCompiler._

  sealed trait AccessToken { def token: String }
  case class AccessIndex(pos: String, token: String) extends AccessToken
  case class AccessKey(key: String, token: String) extends AccessToken
  case object AccessRandom extends AccessToken { val token = ".random" }
  case object AccessSize extends AccessToken { val token = ".size" }

  override def skipWhitespace = false

  var parsedString: String = _

  def parseEl(string: String): List[Part[Any]] = {
    parsedString = string
    parseAll(expr, string) match {
      case Success(parts, _)   => parts
      case Failure(msg, input) => throw new ELParserException(s"Failed to parser $string with error $msg")
    }
  }

  val expr: Parser[List[Part[Any]]] = (multivaluedExpr | elExpr) ^^ {
    case validation: List[Part[Any]] => validation
    case part: Part[Any]             => List(part)
  }

  def multivaluedExpr: Parser[List[Part[Any]]] = (elExpr | staticPart) *

  def staticPart: Parser[StaticPart] = StaticPartPattern ^^ { case staticStr => StaticPart(staticStr) }

  def elExpr: Parser[Part[Any]] = "${" ~> (sessionObject | emptyExpression) <~ "}"

  def sessionObject: Parser[Part[Any]] = objectName ~ ((valueAccess) *) ^^ {
    case objectPart ~ accessTokens =>

      val (part, _) = accessTokens.foldLeft(objectPart.asInstanceOf[Part[Any]] -> objectPart.name)((partName, token) => {
        val (subPart, subPartName) = partName

        val part = token match {
          case AccessIndex(pos, tokenName) => SeqElementPart(subPart, subPartName, pos)
          case AccessKey(key, tokenName)   => MapKeyPart(subPart, subPartName, key)
          case AccessRandom                => RandomPart(subPart, subPartName)
          case AccessSize                  => SizePart(subPart, subPartName)
        }

        val newPartName = subPartName + token.token
        part -> newPartName
      })

      part
  }

  def objectName: Parser[AttributePart] = NamePattern ^^ { case name => AttributePart(name) }

  def valueAccess: Parser[AccessToken] = indexAccess | randomAccess | sizeAccess | keyAccess

  def randomAccess: Parser[AccessToken] = ".random" ^^ { case _ => AccessRandom }

  def sizeAccess: Parser[AccessToken] = ".size" ^^ { case _ => AccessSize }

  def indexAccess: Parser[AccessToken] = "(" ~> NamePattern <~ ")" ^^ { case posStr => AccessIndex(posStr, s"($posStr)") }

  def keyAccess: Parser[AccessToken] = "." ~> NamePattern ^^ { case keyName => AccessKey(keyName, "." + keyName) }

  def emptyExpression: Parser[Part[Any]] = "" ^^ { throw new ELMissingAttributeName(parsedString) }
}
