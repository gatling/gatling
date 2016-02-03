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
package io.gatling.core.session.el

import java.nio.charset.Charset
import java.util.{ Collection => JCollection, List => JList, Map => JMap }

import scala.annotation.tailrec
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scala.util.parsing.combinator.RegexParsers

import io.gatling.commons.NotNothing
import io.gatling.commons.util.TypeCaster
import io.gatling.commons.util.TypeHelper._
import io.gatling.commons.util.StringHelper._
import io.gatling.commons.util.NumberHelper._
import io.gatling.commons.validation._
import io.gatling.core.json.Json
import io.gatling.core.session._

object ElMessages {
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

trait Part[+T] extends Expression[T]

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
      case f: Failure => FalseSuccess
      case _          => TrueSuccess
    }
}

case class IsUndefinedPart(part: Part[Any], name: String) extends Part[Boolean] {
  def apply(session: Session): Validation[Boolean] =
    part(session) match {
      case f: Failure => TrueSuccess
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

  val NameRegex = "[^.${}()]+".r

  val TheELCompiler = new ThreadLocal[ElCompiler] {
    override def initialValue = new ElCompiler
  }

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
        (session: Session) => parts.foldLeft(stringBuilder.success) { (sb, part) =>
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

    sealed trait Bytes { def bytes: Expression[Array[Byte]] }
    case class StaticBytes(bytes: Expression[Array[Byte]]) extends Bytes
    case class DynamicBytes(part: Part[Any]) extends Bytes {
      val bytes: Expression[Array[Byte]] = part.map(_.toString.getBytes(charset))
    }

      @tailrec
      def loop(session: Session, bytes: List[Bytes], resolved: List[Array[Byte]]): Validation[Seq[Array[Byte]]] = bytes match {
        case Nil => resolved.reverse.success
        case head :: tail => head.bytes(session) match {
          case Success(bs)      => loop(session, tail, bs :: resolved)
          case failure: Failure => failure
        }
      }

    val parts = ElCompiler.parse(string)
    val bytes = parts.map {
      case StaticPart(s) =>
        val bs = s.getBytes(charset).success
        StaticBytes(session => bs)
      case part => DynamicBytes(part)
    }

    session: Session => loop(session, bytes, Nil)
  }
}

class ElCompiler extends RegexParsers {

  import ElCompiler._

  sealed trait AccessToken { def token: String }
  case class AccessIndex(pos: String, token: String) extends AccessToken
  case class AccessKey(key: String, token: String) extends AccessToken
  sealed trait AccessFunction extends AccessToken { protected def functionToken(functionName: String) = s".$functionName()" }
  case object AccessRandom extends AccessFunction { val token = functionToken("random") }
  case object AccessSize extends AccessFunction { val token = functionToken("size") }
  case object AccessExists extends AccessFunction { val token = functionToken("exists") }
  case object AccessIsUndefined extends AccessFunction { val token = functionToken("isUndefined") }
  case object AccessJSONStringify extends AccessFunction { val token = functionToken("jsonStringify") }
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

  val expr: Parser[List[Part[Any]]] = multivaluedExpr | (elExpr ^^ { case part: Part[Any] => List(part) })

  def multivaluedExpr: Parser[List[Part[Any]]] = (elExpr | staticPart) *

  val staticPartPattern = new Parser[String] {
    def apply(in: Input) = {
      val source = in.source
      val offset = in.offset
      val end = source.length

        def success(i: Int) = Success(source.subSequence(offset, i).toString, in.drop(i - offset))
        def failure = Failure("Not a static part", in)

      source.indexOf("${", offset) match {
        case -1 if offset == end => failure
        case -1                  => success(end)
        case n if n == offset    => failure
        case n                   => success(n)
      }
    }
  }

  def staticPart: Parser[StaticPart] = staticPartPattern ^^ { case staticStr => StaticPart(staticStr) }

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
          case AccessExists                  => ExistsPart(subPart, subPartName)
          case AccessIsUndefined             => IsUndefinedPart(subPart, subPartName)
          case AccessJSONStringify           => JsonStringify(subPart, subPartName)
          case AccessTuple(index, tokenName) => TupleAccessPart(subPart, subPartName, index.toInt)
        }

        val newPartName = subPartName + token.token
        part -> newPartName
      })

      part
  }) | emptyAttribute

  def objectName: Parser[AttributePart] = NameRegex ^^ { case name => AttributePart(name) }

  def functionAccess(access: AccessFunction) = access.token ^^ { case _ => access }

  def valueAccess =
    tupleAccess |
      indexAccess |
      functionAccess(AccessRandom) |
      functionAccess(AccessSize) |
      functionAccess(AccessExists) |
      functionAccess(AccessIsUndefined) |
      functionAccess(AccessJSONStringify) |
      keyAccess |
      (elExpr ^^ { case _ => throw new Exception("nested attribute definition is not allowed") })

  def indexAccess: Parser[AccessToken] = "(" ~> NameRegex <~ ")" ^^ { case posStr => AccessIndex(posStr, s"($posStr)") }

  def keyAccess: Parser[AccessToken] = "." ~> NameRegex ^^ { case keyName => AccessKey(keyName, "." + keyName) }

  def tupleAccess: Parser[AccessTuple] = "._" ~> "[0-9]+".r ^^ { case indexPart => AccessTuple(indexPart, "._" + indexPart) }

  def emptyAttribute: Parser[Part[Any]] = "" ^^ { case _ => throw new Exception("attribute name is missing") }
}
