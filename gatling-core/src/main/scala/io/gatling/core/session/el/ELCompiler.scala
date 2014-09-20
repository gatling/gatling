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
import java.util.{ Collection => JCollection, List => JList, Map => JMap }

import scala.annotation.tailrec
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.reflect.ClassTag

import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.util.NumberHelper.IntString
import io.gatling.core.util.TypeHelper.TypeCaster
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

import scala.util.parsing.combinator.RegexParsers

object ELMessages {
  def undefinedSeqIndex(name: String, index: Int) = s"Seq named '$name' is undefined for index $index".failure
  def undefinedSessionAttribute(name: String) = s"No attribute named '$name' is defined".failure
  def undefinedMapKey(map: String, key: String) = s"Map named '$map' does not contain key '$key'".failure
  def sizeNotSupported(value: Any, name: String) = s"$value named '$name' does not support .size function".failure
  def accessByKeyNotSupported(value: Any, name: String) = s"$value named '$name' does not support access by key".failure
  def randomNotSupported(value: Any, name: String) = s"$value named '$name' does not support .random function".failure
  def indexAccessNotSupported(value: Any, name: String) = s"$value named '$name' does not support index access".failure
  def outOfRangeAccess(name: String, value: Any, index: Int) = s"Product $value named $name has no element with index $index".failure
  def tupleAccessNotSupported(name: String, value: Any) = s"Product $value named $name do not support tuple access".failure
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
    seqPart(session).flatMap {
      case t: Traversable[_]          => t.size.success
      case collection: JCollection[_] => collection.size.success
      case map: JMap[_, _]            => map.size.success
      case arr: Array[_]              => arr.length.success
      case product: Product           => product.productArity.success
      case other                      => ELMessages.sizeNotSupported(other, name)
    }
}

case class RandomPart(seq: Part[Any], name: String) extends Part[Any] {
  def apply(session: Session): Validation[Any] = {
      def random(size: Int) = ThreadLocalRandom.current.nextInt(size)

    seq(session).flatMap {
      case seq: Seq[_]      => seq(random(seq.size)).success
      case list: JList[_]   => list.get(random(list.size)).success
      case arr: Array[_]    => arr(random(arr.length)).success
      case product: Product => product.productElement(random(product.productArity)).success
      case other            => ELMessages.randomNotSupported(other, name)
    }
  }
}

case class ExistsPart(name: String) extends Part[Boolean] {
  def apply(session: Session): Validation[Boolean] =
    session.contains(name).success
}

case class IsUndefinedPart(name: String) extends Part[Boolean] {
  def apply(session: Session): Validation[Boolean] =
    (!session.contains(name)).success
}

case class SeqElementPart(seq: Part[Any], seqName: String, index: String) extends Part[Any] {
  def apply(session: Session): Validation[Any] = {

      def seqElementPart(index: Int): Validation[Any] = seq(session).flatMap {
        case seq: Seq[_] =>
          if (seq.isDefinedAt(index)) seq(index).success
          else ELMessages.undefinedSeqIndex(seqName, index)

        case arr: Array[_] =>
          if (index < arr.length) arr(index).success
          else ELMessages.undefinedSeqIndex(seqName, index)

        case list: JList[_] =>
          if (index < list.size) list.get(index).success
          else ELMessages.undefinedSeqIndex(seqName, index)

        case other => ELMessages.indexAccessNotSupported(other, seqName)
      }

    index match {
      case IntString(i) => seqElementPart(i)
      case _            => session(index).validate[Int].flatMap(seqElementPart)
    }
  }
}

case class MapKeyPart(map: Part[Any], mapName: String, key: String) extends Part[Any] {

  def apply(session: Session): Validation[Any] = map(session).flatMap {
    case m: Map[_, _] => m.asInstanceOf[Map[Any, _]].get(key) match {
      case Some(value) => value.success
      case None        => ELMessages.undefinedMapKey(mapName, key)
    }

    case map: JMap[_, _] =>
      if (map.containsKey(key)) map.get(key).success
      else ELMessages.undefinedMapKey(mapName, key)

    case other => ELMessages.accessByKeyNotSupported(other, mapName)
  }
}

case class TupleAccessPart(tuple: Part[Any], tupleName: String, index: Int) extends Part[Any] {
  def apply(session: Session): Validation[Any] = tuple(session).flatMap {
    case product: Product =>
      if (index > 0 && product.productArity >= index) product.productElement(index - 1).success
      else ELMessages.outOfRangeAccess(tupleName, product, index)

    case other => ELMessages.tupleAccessNotSupported(tupleName, other)
  }
}

class ELParserException(string: String, msg: String) extends Exception(s"Failed to parse $string with error '$msg'")

object ELCompiler {

  val StaticPartPattern = """(?s).+?(?!$\{)""".r
  val NamePattern = "[^.${}()]+".r

  val ElCompiler = new ThreadLocal[ELCompiler] {
    override def initialValue = new ELCompiler
  }

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
            case StaticPart(s) => sb.map(_.append(s))
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
  case object AccessRandom extends AccessToken { val token = ".random()" }
  case object AccessSize extends AccessToken { val token = ".size()" }
  case object AccessExists extends AccessToken { val token = ".exists()" }
  case object AccessIsUndefined extends AccessToken { val token = ".isUndefined()" }
  case class AccessTuple(index: String, token: String) extends AccessToken

  override def skipWhitespace = false

  // FIXME remove once #2224 is fixed
  private def mergeConsecutiveStaticParts(parts: List[Part[Any]]): List[Part[Any]] = {

      @tailrec
      def merge(parts: List[Part[Any]], currentStaticPart: Option[String], acc: List[Part[Any]]): List[Part[Any]] =
        parts match {
          case Nil => currentStaticPart match {
            case None         => acc
            case Some(string) => StaticPart(string) :: acc
          }

          case head :: tail => head match {
            case StaticPart(s) =>
              currentStaticPart match {
                case None         => merge(tail, Some(s), acc)
                case Some(string) => merge(tail, Some(string + s), acc)
              }

            case part =>
              currentStaticPart match {
                case None         => merge(tail, None, part :: acc)
                case Some(string) => merge(tail, None, part :: StaticPart(string) :: acc)
              }
          }
        }

    merge(parts, None, Nil).reverse
  }

  def parseEl(string: String): List[Part[Any]] =
    try {
      parseAll(expr, string) match {
        case Success(parts, _) => mergeConsecutiveStaticParts(parts)
        case ns: NoSuccess     => throw new ELParserException(string, ns.msg)
      }
    } catch {
      case e: Exception => throw new ELParserException(string, e.getMessage)
    }

  val expr: Parser[List[Part[Any]]] = multivaluedExpr | (elExpr ^^ { case part: Part[Any] => List(part) })

  def multivaluedExpr: Parser[List[Part[Any]]] = (elExpr | staticPart) *

  def staticPart: Parser[StaticPart] = StaticPartPattern ^^ { case staticStr => StaticPart(staticStr) }

  def elExpr: Parser[Part[Any]] = "${" ~> sessionObject <~ "}"

  def sessionObject: Parser[Part[Any]] = (objectName ~ (valueAccess *) ^^ {
    case objectPart ~ accessTokens =>

      val (part, _) = accessTokens.foldLeft(objectPart.asInstanceOf[Part[Any]] -> objectPart.name)((partName, token) => {
        val (subPart, subPartName) = partName

        val part = token match {
          case AccessIndex(pos, tokenName)   => SeqElementPart(subPart, subPartName, pos)
          case AccessKey(key, tokenName)     => MapKeyPart(subPart, subPartName, key)
          case AccessRandom                  => RandomPart(subPart, subPartName)
          case AccessSize                    => SizePart(subPart, subPartName)
          case AccessExists                  => ExistsPart(subPartName)
          case AccessIsUndefined             => IsUndefinedPart(subPartName)
          case AccessTuple(index, tokenName) => TupleAccessPart(subPart, subPartName, index.toInt)
        }

        val newPartName = subPartName + token.token
        part -> newPartName
      })

      part
  }) | emptyAttribute

  def objectName: Parser[AttributePart] = NamePattern ^^ { case name => AttributePart(name) }

  def functionAccess(access: AccessToken) = access.token ^^ { case _ => access }

  def valueAccess =
    tupleAccess |
      indexAccess |
      functionAccess(AccessRandom) |
      functionAccess(AccessSize) |
      functionAccess(AccessExists) |
      functionAccess(AccessIsUndefined) |
      keyAccess |
      (elExpr ^^ { case _ => throw new Exception("nested attribute definition is not allowed") })

  def indexAccess: Parser[AccessToken] = "(" ~> NamePattern <~ ")" ^^ { case posStr => AccessIndex(posStr, s"($posStr)") }

  def keyAccess: Parser[AccessToken] = "." ~> NamePattern ^^ { case keyName => AccessKey(keyName, "." + keyName) }

  def tupleAccess: Parser[AccessTuple] = "._" ~> "[0-9]+".r ^^ { case indexPart => AccessTuple(indexPart, "._" + indexPart) }

  def emptyAttribute: Parser[Part[Any]] = "" ^^ { case _ => throw new Exception("attribute name is missing") }
}
