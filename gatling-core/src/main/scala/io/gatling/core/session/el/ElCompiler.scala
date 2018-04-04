/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.core.session.el

import java.nio.charset.Charset
import java.util.concurrent.ThreadLocalRandom
import java.util.{ Collection => JCollection, List => JList, Map => JMap }

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scala.util.parsing.combinator.RegexParsers

import io.gatling.commons.NotNothing
import io.gatling.commons.util.NumberHelper._
import io.gatling.commons.util.StringHelper._
import io.gatling.commons.util.TypeHelper._
import io.gatling.commons.util.TypeCaster
import io.gatling.commons.validation._
import io.gatling.core.json.Json
import io.gatling.core.session._
import io.gatling.netty.util.ahc.StringBuilderPool

object ElMessages {
  def undefinedSeqIndex(name: String, index: Int): Failure = s"Seq named '$name' is undefined for index $index".failure
  def undefinedSessionAttribute(name: String): Failure = s"No attribute named '$name' is defined".failure
  def undefinedMapKey(map: String, key: String): Failure = s"Map named '$map' does not contain key '$key'".failure
  def sizeNotSupported(value: Any, name: String): Failure = s"$value named '$name' does not support .size function".failure
  def accessByKeyNotSupported(value: Any, name: String): Failure = s"$value named '$name' does not support access by key".failure
  def randomNotSupported(value: Any, name: String): Failure = s"$value named '$name' does not support .random function".failure
  def indexAccessNotSupported(value: Any, name: String): Failure = s"$value named '$name' does not support index access".failure
  def outOfRangeAccess(name: String, value: Any, index: Int): Failure = s"Product $value named $name has no element with index $index".failure
  def tupleAccessNotSupported(name: String, value: Any): Failure = s"Product $value named $name do not support tuple access".failure
}

sealed trait Part[+T] extends (Session => Validation[T])

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
      case other                      => ElMessages.sizeNotSupported(other, name)
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
      case other            => ElMessages.randomNotSupported(other, name)
    }
  }
}

case class ExistsPart(part: Part[Any], name: String) extends Part[Boolean] {
  def apply(session: Session): Validation[Boolean] =
    part(session) match {
      case _: Failure => FalseSuccess
      case _          => TrueSuccess
    }
}

case class IsUndefinedPart(part: Part[Any], name: String) extends Part[Boolean] {
  def apply(session: Session): Validation[Boolean] =
    part(session) match {
      case _: Failure => TrueSuccess
      case _          => FalseSuccess
    }
}

case class JsonStringify(part: Part[Any], name: String) extends Part[String] {
  def apply(session: Session): Validation[String] =
    part(session) match {
      case Success(value)   => Json.stringify(value, isRootObject = false).success
      case NullValueFailure => NullStringSuccess
      case failure: Failure => failure
    }
}

case class SeqElementPart(seq: Part[Any], seqName: String, index: String) extends Part[Any] {
  def apply(session: Session): Validation[Any] = {

    def seqElementPart(index: Int): Validation[Any] = seq(session).flatMap {
      case seq: Seq[_] =>
        if (seq.isDefinedAt(index)) seq(index).success
        else ElMessages.undefinedSeqIndex(seqName, index)

      case arr: Array[_] =>
        if (index < arr.length) arr(index).success
        else ElMessages.undefinedSeqIndex(seqName, index)

      case list: JList[_] =>
        if (index < list.size) list.get(index).success
        else ElMessages.undefinedSeqIndex(seqName, index)

      case other => ElMessages.indexAccessNotSupported(other, seqName)
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
      case None        => ElMessages.undefinedMapKey(mapName, key)
    }

    case map: JMap[_, _] =>
      if (map.containsKey(key)) map.get(key).success
      else ElMessages.undefinedMapKey(mapName, key)

    case other => ElMessages.accessByKeyNotSupported(other, mapName)
  }
}

case class TupleAccessPart(tuple: Part[Any], tupleName: String, index: Int) extends Part[Any] {
  def apply(session: Session): Validation[Any] = tuple(session).flatMap {
    case product: Product =>
      if (index > 0 && product.productArity >= index) product.productElement(index - 1).success
      else ElMessages.outOfRangeAccess(tupleName, product, index)

    case other => ElMessages.tupleAccessNotSupported(tupleName, other)
  }
}

class ElParserException(string: String, msg: String) extends Exception(s"Failed to parse $string with error '$msg'")

object ElCompiler {

  private val NameRegex = "[^.${}()]+".r
  private val DynamicPartStart = "${".toCharArray

  private val TheELCompiler = new ThreadLocal[ElCompiler] {
    override def initialValue = new ElCompiler
  }

  private val EmptyBytesExpression: Expression[Seq[Array[Byte]]] = Seq(Array.empty[Byte]).expressionSuccess

  def parse(string: String): List[Part[Any]] = TheELCompiler.get.parseEl(string)

  def compile[T: TypeCaster: ClassTag: NotNothing](string: String): Expression[T] =
    parse(string) match {
      case List(StaticPart(staticStr)) =>
        val runtimeClass = implicitly[ClassTag[T]].runtimeClass
        if (runtimeClass == classOf[String] || runtimeClass == classOf[Any] || runtimeClass == classOf[Object]) {
          StaticStringExpression(staticStr).asInstanceOf[Expression[T]]
        } else {
          val stringV = staticStr.asValidation[T]
          _ => stringV
        }

      case List(dynamicPart) => dynamicPart(_).flatMap(_.asValidation[T])

      case parts =>
        (session: Session) => parts.foldLeft(StringBuilderPool.DEFAULT.get().success) { (sb, part) =>
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

  def compile2BytesSeq(string: String, charset: Charset): Expression[Seq[Array[Byte]]] = {

    @tailrec
    def compile2BytesSeqRec(session: Session, bytes: List[Bytes], resolved: List[Array[Byte]]): Validation[Seq[Array[Byte]]] =
      bytes match {
        case Nil => resolved.reverse.success
        case head :: tail => head.bytes(session) match {
          case Success(bs)      => compile2BytesSeqRec(session, tail, bs :: resolved)
          case failure: Failure => failure
        }
      }

    val parts = ElCompiler.parse(string)

    parts match {
      case Nil                  => EmptyBytesExpression
      case StaticPart(s) :: Nil => Seq(s.getBytes(charset)).expressionSuccess
      case _ =>
        val bytes = parts.map {
          case StaticPart(s) => StaticBytes(s.getBytes(charset).expressionSuccess)
          case part          => DynamicBytes(part, charset)
        }
        session: Session => compile2BytesSeqRec(session, bytes, Nil)
    }
  }
}

class ElCompiler extends RegexParsers {

  import ElCompiler._

  sealed trait AccessToken { def token: String }
  case class AccessIndex(pos: String, token: String) extends AccessToken
  case class AccessKey(key: String, token: String) extends AccessToken
  sealed trait AccessFunction extends AccessToken { protected def functionToken(functionName: String) = s".$functionName()" }
  case object AccessRandom extends AccessFunction { val token: String = functionToken("random") }
  case object AccessSize extends AccessFunction { val token: String = functionToken("size") }
  case object AccessExists extends AccessFunction { val token: String = functionToken("exists") }
  case object AccessIsUndefined extends AccessFunction { val token: String = functionToken("isUndefined") }
  case object AccessJSONStringify extends AccessFunction { val token: String = functionToken("jsonStringify") }
  case class AccessTuple(index: String, token: String) extends AccessToken

  override def skipWhitespace = false

  def parseEl(string: String): List[Part[Any]] = {

    val parseResult =
      try { parseAll(expr, string) }
      catch { case NonFatal(e) => throw new ElParserException(string, e.getMessage) }

    parseResult match {
      case Success(parts, _) => parts
      case ns: NoSuccess     => throw new ElParserException(string, ns.msg)
    }
  }

  private val expr: Parser[List[Part[Any]]] = multivaluedExpr | (elExpr ^^ { part => List(part) })

  private def multivaluedExpr: Parser[List[Part[Any]]] = (elExpr | staticPart) *

  private val staticPartPattern = new Parser[String] {
    override def apply(in: Input): ParseResult[String] = {
      val source = in.source
      val offset = in.offset
      val end = source.length

      def success(i: Int) = Success(source.subSequence(offset, i).toString, in.drop(i - offset))
      def failure = Failure("Not a static part", in)

      source.indexOf(DynamicPartStart, offset) match {
        case -1 if offset == end => failure
        case -1                  => success(end)
        case n if n == offset    => failure
        case n                   => success(n)
      }
    }
  }

  private def staticPart: Parser[StaticPart] = staticPartPattern ^^ { staticStr => StaticPart(staticStr) }

  private def elExpr: Parser[Part[Any]] = "${" ~> sessionObject <~ "}"

  private def sessionObject: Parser[Part[Any]] = {

    @tailrec
    def sessionObjectRec(accessTokens: List[AccessToken], currentPart: Part[Any], currentPartName: String): Part[Any] = {
      accessTokens match {
        case Nil => currentPart
        case token :: otherTokens =>
          val newPart =
            token match {
              case AccessIndex(pos, _)   => SeqElementPart(currentPart, currentPartName, pos)
              case AccessKey(key, _)     => MapKeyPart(currentPart, currentPartName, key)
              case AccessRandom          => RandomPart(currentPart, currentPartName)
              case AccessSize            => SizePart(currentPart, currentPartName)
              case AccessExists          => ExistsPart(currentPart, currentPartName)
              case AccessIsUndefined     => IsUndefinedPart(currentPart, currentPartName)
              case AccessJSONStringify   => JsonStringify(currentPart, currentPartName)
              case AccessTuple(index, _) => TupleAccessPart(currentPart, currentPartName, index.toInt)
            }

          val newPartName = currentPartName + token.token
          sessionObjectRec(otherTokens, newPart, newPartName)
      }
    }

    (objectName ~ (valueAccess *) ^^ { case objectPart ~ accessTokens => sessionObjectRec(accessTokens, objectPart, objectPart.name) }) | emptyAttribute
  }

  private def objectName: Parser[AttributePart] = NameRegex ^^ { name => AttributePart(name) }

  private def functionAccess(access: AccessFunction) = access.token ^^ { _ => access }

  private def valueAccess =
    tupleAccess |
      indexAccess |
      functionAccess(AccessRandom) |
      functionAccess(AccessSize) |
      functionAccess(AccessExists) |
      functionAccess(AccessIsUndefined) |
      functionAccess(AccessJSONStringify) |
      keyAccess |
      (elExpr ^^ { _ => throw new Exception("nested attribute definition is not allowed") })

  private def indexAccess: Parser[AccessToken] = "(" ~> NameRegex <~ ")" ^^ { posStr => AccessIndex(posStr, s"($posStr)") }

  private def keyAccess: Parser[AccessToken] = "." ~> NameRegex ^^ { keyName => AccessKey(keyName, "." + keyName) }

  private def tupleAccess: Parser[AccessTuple] = "._" ~> "[0-9]+".r ^^ { indexPart => AccessTuple(indexPart, "._" + indexPart) }

  private def emptyAttribute: Parser[Part[Any]] = "" ^^ { _ => throw new Exception("attribute name is missing") }
}

sealed trait Bytes { def bytes: Expression[Array[Byte]] }
case class StaticBytes(bytes: Expression[Array[Byte]]) extends Bytes
case class DynamicBytes(part: Part[Any], charset: Charset) extends Bytes {
  val bytes: Expression[Array[Byte]] = part.map(_.toString.getBytes(charset))
}
