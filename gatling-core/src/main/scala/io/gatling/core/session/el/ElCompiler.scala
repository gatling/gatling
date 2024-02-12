/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

import java.{ lang => jl, util => ju }
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import scala.annotation.tailrec
import scala.reflect.ClassTag
import scala.util.control.NonFatal
import scala.util.parsing.combinator.RegexParsers

import io.gatling.commons.NotNothing
import io.gatling.commons.util.StringHelper._
import io.gatling.commons.util.TypeCaster
import io.gatling.commons.util.TypeHelper
import io.gatling.commons.validation._
import io.gatling.core.json.Json
import io.gatling.core.session._
import io.gatling.core.util.Html
import io.gatling.shared.util.StringBuilderPool

import com.typesafe.scalalogging.StrictLogging
import io.github.metarank.cfor._

object ElMessages {
  def undefinedSeqIndex(name: String, index: Int): Failure = s"Seq named '$name' is undefined for index $index".failure
  def undefinedSessionAttribute(name: String): Failure = s"No attribute named '$name' is defined".failure
  def undefinedMapKey(map: String, key: String): Failure = s"Map named '$map' does not contain key '$key'".failure
  def sizeNotSupported(value: Any, name: String): Failure = s"$value named '$name' does not support .size function".failure
  def accessByKeyNotSupported(value: Any, name: String): Failure = s"$value named '$name' does not support access by key".failure
  def randomNotSupported(value: Any, name: String): Failure = s"$value named '$name' does not support .random function".failure
  def indexAccessNotSupported(value: Any, name: String): Failure = s"$value named '$name' does not support index access".failure
  def outOfRangeAccess(name: String, value: Any, index: Int): Failure = s"Product $value named $name has no element with index $index".failure
  def tupleAccessNotSupported(name: String, value: Any): Failure = s"$value named $name do not support tuple access".failure
  def htmlUnescapeNotSupported(value: Any, name: String): Failure = s"$value named '$name' does not support .htmlUnescape function".failure
}

sealed trait ElPart[+T] extends (Session => Validation[T]) with Product with Serializable

final case class StaticPart(string: String) extends ElPart[String] {
  def apply(session: Session): Validation[String] = string.success
}

final case class AttributePart(name: String) extends ElPart[Any] {
  def apply(session: Session): Validation[Any] = session(name).validate[Any]
}

final case class SizePart(seqPart: ElPart[Any], name: String) extends ElPart[Int] {
  def apply(session: Session): Validation[Int] =
    seqPart(session).flatMap {
      case t: Iterable[_]               => t.size.success
      case collection: ju.Collection[_] => collection.size.success
      case map: ju.Map[_, _]            => map.size.success
      case arr: Array[_]                => arr.length.success
      case product: Product             => product.productArity.success
      case other                        => ElMessages.sizeNotSupported(other, name)
    }
}

final case class RandomPart(seq: ElPart[Any], name: String) extends ElPart[Any] {
  @SuppressWarnings(Array("org.wartremover.warts.SeqApply"))
  def apply(session: Session): Validation[Any] = {
    def random(size: Int) = ThreadLocalRandom.current.nextInt(size)

    seq(session).flatMap {
      case seq: Seq[_]      => seq(random(seq.size)).success
      case list: ju.List[_] => list.get(random(list.size)).success
      case arr: Array[_]    => arr(random(arr.length)).success
      case product: Product => product.productElement(random(product.productArity)).success
      case other            => ElMessages.randomNotSupported(other, name)
    }
  }
}

final case class ExistsPart(part: ElPart[Any], name: String) extends ElPart[Boolean] {
  def apply(session: Session): Validation[Boolean] =
    part(session) match {
      case _: Failure => Validation.FalseSuccess
      case _          => Validation.TrueSuccess
    }
}

final case class IsUndefinedPart(part: ElPart[Any], name: String) extends ElPart[Boolean] {
  def apply(session: Session): Validation[Boolean] =
    part(session) match {
      case _: Failure => Validation.TrueSuccess
      case _          => Validation.FalseSuccess
    }
}

final case class JsonStringify(part: ElPart[Any], name: String) extends ElPart[String] {
  def apply(session: Session): Validation[String] =
    part(session) match {
      case Success(value)   => Json.stringify(value, isRootObject = false).success
      case failure: Failure => if (TypeHelper.isNullValueFailure(failure)) Validation.NullStringSuccess else failure
    }
}

final case class HtmlUnescape(part: ElPart[Any], name: String) extends ElPart[String] {
  def apply(session: Session): Validation[String] =
    part(session).flatMap {
      case s: String => Success(Html.unescape(s))
      case other     => ElMessages.htmlUnescapeNotSupported(other, name)
    }
}

private class IntStringOpt(val s: String) extends AnyVal {
  def isEmpty: Boolean =
    s.toCharArray.zipWithIndex.exists { case (char, i) =>
      (char < '0' || char > '9') && !(char == '-' && i == 0)
    }

  def get: Int = s.toInt
}

private object IntString {
  def unapply(s: String): IntStringOpt = new IntStringOpt(s)
}

final case class SeqElementPart(seq: ElPart[Any], seqName: String, index: String) extends ElPart[Any] {
  def apply(session: Session): Validation[Any] = {
    @SuppressWarnings(Array("org.wartremover.warts.SeqApply"))
    def seqElementPart(index: Int): Validation[Any] = seq(session).flatMap {
      case seq: Seq[_] =>
        if (seq.isDefinedAt(index)) {
          seq(index).success
        } else if (index < 0) {
          val backwardIndex = seq.length + index
          if (seq.isDefinedAt(backwardIndex)) {
            seq(backwardIndex).success
          } else {
            ElMessages.undefinedSeqIndex(seqName, index)
          }
        } else {
          ElMessages.undefinedSeqIndex(seqName, index)
        }

      case arr: Array[_] =>
        val actualIndex = if (index >= 0) index else arr.length + index
        if (actualIndex < arr.length) {
          arr(actualIndex).success
        } else {
          ElMessages.undefinedSeqIndex(seqName, index)
        }

      case list: ju.List[_] =>
        val actualIndex = if (index >= 0) index else list.size + index
        if (actualIndex < list.size) {
          list.get(actualIndex).success
        } else {
          ElMessages.undefinedSeqIndex(seqName, index)
        }

      case product: Product =>
        val actualIndex = if (index >= 0) index else product.productArity + index
        if (actualIndex < product.productArity) {
          product.productElement(actualIndex).success
        } else {
          ElMessages.outOfRangeAccess(seqName, product, index)
        }

      case other => ElMessages.indexAccessNotSupported(other, seqName)
    }

    index match {
      case IntString(i) => seqElementPart(i)
      case _            => session(index).validate[Int].flatMap(seqElementPart)
    }
  }
}

final case class MapKeyPart(map: ElPart[Any], mapName: String, key: String) extends ElPart[Any] {
  @SuppressWarnings(Array("org.wartremover.warts.Return"))
  private def lookup(product: Product): Validation[Any] = {
    cfor(0 until product.productArity) { i =>
      if (product.productElementName(i) == key) {
        return product.productElement(i).success
      }
    }
    ElMessages.undefinedMapKey(mapName, key)
  }

  def apply(session: Session): Validation[Any] = map(session).flatMap {
    case m: Map[_, _] =>
      m.asInstanceOf[Map[Any, _]].get(key) match {
        case Some(value) => value.success
        case _           => ElMessages.undefinedMapKey(mapName, key)
      }

    case map: ju.Map[_, _] =>
      if (map.containsKey(key)) map.get(key).success
      else ElMessages.undefinedMapKey(mapName, key)

    case product: Product => lookup(product)

    case other =>
      Pojos.getProperty(other, key) match {
        case _: Failure => ElMessages.accessByKeyNotSupported(other, mapName)
        case success    => success
      }
  }
}

final case class TupleAccessPart(tuple: ElPart[Any], tupleName: String, index: Int) extends ElPart[Any] {
  def apply(session: Session): Validation[Any] = tuple(session).flatMap {
    case product: Product =>
      if (index > 0 && product.productArity >= index) product.productElement(index - 1).success
      else ElMessages.outOfRangeAccess(tupleName, product, index)

    case other => ElMessages.tupleAccessNotSupported(tupleName, other)
  }
}

case object CurrentTimeMillisPart extends ElPart[Long] {
  def apply(session: Session): Validation[Long] = System.currentTimeMillis().success
}

final case class CurrentDateTimePart(format: DateTimeFormatter) extends ElPart[String] {
  def apply(session: Session): Validation[String] = format.format(ZonedDateTime.now()).success
}

case object RandomSecureUUID extends ElPart[String] {
  def apply(session: Session): Validation[String] = UUID.randomUUID().toString.success
}

case object RandomUUID extends ElPart[String] {
  private val Version4Mask = 2L << 62
  private val VariantMask = 2L << 62

  def version4UUID(): UUID = {
    val rnd = ThreadLocalRandom.current()
    val mostSigBits = (rnd.nextLong() & ~0xf000L) | Version4Mask
    val leastSigBits = ((rnd.nextLong() << 2) >>> 2) | VariantMask
    new UUID(mostSigBits, leastSigBits)
  }

  def apply(session: Session): Validation[String] = version4UUID().toString.success
}

case object RandomInt extends ElPart[Int] {
  def apply(session: Session): Validation[Int] = ThreadLocalRandom.current().nextInt().success
}

final case class RandomIntRange(min: Int, max: Int) extends ElPart[Int] {
  require(min < max, s"Range 'max'($max) must be above than 'min'($min)")
  def apply(session: Session): Validation[Int] = ThreadLocalRandom.current().nextInt(min, max).success
}

case object RandomLong extends ElPart[Long] {
  def apply(session: Session): Validation[Long] = ThreadLocalRandom.current().nextLong().success
}

final case class RandomLongRange(min: Long, max: Long) extends ElPart[Long] {
  require(min < max, s"Range 'max'($max) must be above than 'min'($min)")
  def apply(session: Session): Validation[Long] = ThreadLocalRandom.current().nextLong(min, max).success
}

final case class RandomDoubleRange(min: Double, max: Double) extends ElPart[Double] {
  require(min < max, s"'max'($max) should be greater than 'min'($min)")
  def apply(session: Session): Validation[Double] =
    ThreadLocalRandom.current().nextDouble(min, max).success
}

final case class RandomDoubleRangeDigits(min: Double, max: Double, fractionalDigits: Int) extends ElPart[Double] {
  require(min < max, s"'max'($max) should be greater than 'min'($min)")
  require(fractionalDigits >= 0, s"fractionalDigits($fractionalDigits) must be positive")

  private val tens = Math.pow(10.0, fractionalDigits)
  def apply(session: Session): Validation[Double] =
    ((ThreadLocalRandom.current().nextDouble(min, max) * tens).round / tens).success
}

object RandomAlphanumeric {
  private val AlphanumericChars: Array[Char] = (('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')).toArray
}

final case class RandomAlphanumeric(length: Int) extends ElPart[String] {
  require(length > 0, s"'length' should be greater than 0")

  def apply(session: Session): Validation[String] = {
    val sb = new jl.StringBuilder(length)
    val rng = ThreadLocalRandom.current()
    cfor(0 until length) { _ =>
      sb.append(RandomAlphanumeric.AlphanumericChars(rng.nextInt(RandomAlphanumeric.AlphanumericChars.length)))
    }
    sb.toString.success
  }
}

final class ElParserException(string: String, msg: String) extends Exception(s"Failed to parse $string with error '$msg'")

object ElCompiler extends StrictLogging {
  private val NameRegex = """[^.#{}()]+""".r
  private val DateFormatRegex = """[^#{}()]+""".r
  private val NumberRegex = """\d+""".r
  private val NumberRegexWithNegative = """-?\d+""".r
  private val DecimalRegexWithNegative = """-?\d+\.\d+""".r
  private val DynamicPartStart = "#{".toCharArray

  private val ElCompilers = new ThreadLocal[ElCompiler] {
    override def initialValue = new ElCompiler
  }

  @throws[ElParserException]
  def parse(string: String): List[ElPart[Any]] =
    ElCompilers.get.parseEl(string)

  def compile[T: TypeCaster: ClassTag: NotNothing](string: String): Expression[T] =
    parse(string) match {
      case StaticPart(staticStr) :: Nil =>
        val runtimeClass = implicitly[ClassTag[T]].runtimeClass
        if (runtimeClass == classOf[String] || runtimeClass == classOf[Any] || runtimeClass == classOf[Object]) {
          StaticValueExpression(staticStr).asInstanceOf[Expression[T]]
        } else {
          val stringV = TypeHelper.validate[T](staticStr)
          _ => stringV
        }

      case dynamicPart :: Nil => dynamicPart(_).flatMap(TypeHelper.validate[T])

      case parts =>
        (session: Session) =>
          parts
            .foldLeft(StringBuilderPool.DEFAULT.get().success) { (sb, part) =>
              part match {
                case StaticPart(s) => sb.map(_.append(s))
                case _ =>
                  for {
                    sb <- sb
                    part <- part(session)
                  } yield sb.append(part)
              }
            }
            .flatMap(value => TypeHelper.validate[T](value.toString))
    }
}

private[el] sealed trait AccessToken extends Product with Serializable { def token: String }
private[el] final case class AccessIndex(pos: String, token: String) extends AccessToken
private[el] final case class AccessKey(key: String, token: String) extends AccessToken
private[el] sealed trait AccessFunction extends AccessToken { protected def functionToken(functionName: String) = s".$functionName()" }
private[el] case object AccessRandom extends AccessFunction { val token: String = functionToken("random") }
private[el] case object AccessSize extends AccessFunction { val token: String = functionToken("size") }
private[el] case object AccessExists extends AccessFunction { val token: String = functionToken("exists") }
private[el] case object AccessIsUndefined extends AccessFunction { val token: String = functionToken("isUndefined") }
private[el] case object AccessJsonStringify extends AccessFunction { val token: String = functionToken("jsonStringify") }
private[el] case object AccessHtmlUnescape extends AccessFunction { val token: String = functionToken("htmlUnescape") }
private[el] final case class AccessTuple(index: String, token: String) extends AccessToken

final class ElCompiler private extends RegexParsers {
  import ElCompiler._

  override def skipWhitespace = false

  private def parseEl(string: String): List[ElPart[Any]] = {
    val parseResult =
      try {
        parseAll(expr, string)
      } catch { case NonFatal(e) => throw new ElParserException(string, e.getMessage) }

    parseResult match {
      case Success(parts, _) => parts
      case ns: NoSuccess     => throw new ElParserException(string, ns.msg)
    }
  }

  private val expr: Parser[List[ElPart[Any]]] = multivaluedExpr | elExpr.^^(_ :: Nil)

  private def multivaluedExpr: Parser[List[ElPart[Any]]] = (elExpr | staticPart).*

  private val staticPartPattern: Parser[List[String]] = new Parser[String] {
    override def apply(in: Input): ParseResult[String] = {
      val source = in.source
      val offset = in.offset
      val end = source.length

      def success(i: Int) = Success(source.subSequence(offset, i).toString, in.drop(i - offset))

      source.indexOf(DynamicPartStart, offset) match {
        case -1 => success(end)
        case n =>
          if (n - 1 >= offset && source.charAt(n - 1) == '\\') {
            Success("#{", in.drop(n - offset + 2))
          } else {
            success(n)
          }
      }
    }
  } >> {
    case "" => success(Nil)
    case s  => staticPartPattern ^^ (s :: _)
  }

  private def staticPart: Parser[StaticPart] =
    staticPartPattern.^?(
      {
        case staticStr if staticStr.nonEmpty => StaticPart(staticStr.mkString)
      },
      _ => "Not a static part"
    )

  private def elExpr: Parser[ElPart[Any]] = "#{" ~> (nonSessionObject | sessionObject | emptyAttribute) <~ "}"

  private def currentTimeMillis: Parser[ElPart[Any]] = "currentTimeMillis()" ^^ (_ => CurrentTimeMillisPart)

  private def currentDate: Parser[ElPart[Any]] =
    "currentDate(" ~> DateFormatRegex <~ ")" ^^ (format => CurrentDateTimePart(DateTimeFormatter.ofPattern(format)))

  private def randomSecureUuid: Parser[ElPart[Any]] = "randomSecureUuid()" ^^ (_ => RandomSecureUUID)

  private def randomUuid: Parser[ElPart[Any]] = "randomUuid()" ^^ (_ => RandomUUID)

  private def randomInt: Parser[ElPart[Any]] = "randomInt()" ^^ (_ => RandomInt)

  private def randomIntRange: Parser[ElPart[Any]] = "randomInt(" ~> NumberRegexWithNegative ~ ("," ~> NumberRegexWithNegative) <~ ")" ^^ { case min ~ max =>
    RandomIntRange(min.toInt, max.toInt)
  }

  private def randomLong: Parser[ElPart[Any]] = "randomLong()" ^^ (_ => RandomLong)

  private def randomLongRange: Parser[ElPart[Any]] = "randomLong(" ~> NumberRegexWithNegative ~ ("," ~> NumberRegexWithNegative) <~ ")" ^^ { case min ~ max =>
    RandomLongRange(min.toLong, max.toLong)
  }

  private def randomDoubleRange: Parser[ElPart[Any]] = "randomDouble(" ~> DecimalRegexWithNegative ~ ("," ~> DecimalRegexWithNegative) <~ ")" ^^ {
    case min ~ max =>
      RandomDoubleRange(min.toDouble, max.toDouble)
  }

  private def randomDoubleRangeDigits: Parser[ElPart[Any]] =
    "randomDouble(" ~> DecimalRegexWithNegative ~ ("," ~> DecimalRegexWithNegative) ~ ("," ~> NumberRegex) <~ ")" ^^ { case min ~ max ~ digit =>
      RandomDoubleRangeDigits(min.toDouble, max.toDouble, digit.toInt)
    }

  private def randomAlphanumeric: Parser[ElPart[Any]] =
    "randomAlphanumeric(" ~> NumberRegex <~ ")" ^^ (length => RandomAlphanumeric(length.toInt))

  private def nonSessionObject: Parser[ElPart[Any]] =
    currentTimeMillis | currentDate | randomUuid | randomSecureUuid | randomInt | randomIntRange | randomLong | randomLongRange | randomDoubleRange | randomDoubleRangeDigits | randomAlphanumeric

  private def indexAccess: Parser[AccessToken] = "(" ~> NameRegex <~ ")" ^^ (posStr => AccessIndex(posStr, s"($posStr)"))

  private def keyAccess: Parser[AccessToken] = "." ~> NameRegex ^^ (keyName => AccessKey(keyName, "." + keyName))

  private def tupleAccess: Parser[AccessTuple] = "._" ~> NumberRegex ^^ (indexPart => AccessTuple(indexPart, "._" + indexPart))

  private def emptyAttribute: Parser[ElPart[Any]] = "" ^^ (_ => throw new Exception("attribute name is missing"))

  private def sessionObject: Parser[ElPart[Any]] = {
    @tailrec
    def sessionObjectRec(accessTokens: List[AccessToken], currentPart: ElPart[Any], currentPartName: String): ElPart[Any] =
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
              case AccessJsonStringify   => JsonStringify(currentPart, currentPartName)
              case AccessHtmlUnescape    => HtmlUnescape(currentPart, currentPartName)
              case AccessTuple(index, _) => TupleAccessPart(currentPart, currentPartName, index.toInt)
            }

          val newPartName = currentPartName + token.token
          sessionObjectRec(otherTokens, newPart, newPartName)
      }

    objectName ~ valueAccess.* ^^ { case objectPart ~ accessTokens => sessionObjectRec(accessTokens, objectPart, objectPart.name) }
  }

  private def objectName: Parser[AttributePart] = NameRegex ^^ AttributePart

  private def functionAccess(access: AccessFunction): Parser[AccessFunction] = access.token ^^ (_ => access)

  private def valueAccess: Parser[AccessToken] =
    tupleAccess |
      indexAccess |
      functionAccess(AccessRandom) |
      functionAccess(AccessSize) |
      functionAccess(AccessExists) |
      functionAccess(AccessIsUndefined) |
      functionAccess(AccessJsonStringify) |
      functionAccess(AccessHtmlUnescape) |
      keyAccess |
      (elExpr ^^ (_ => throw new Exception("nested attribute definition is not allowed")))
}
