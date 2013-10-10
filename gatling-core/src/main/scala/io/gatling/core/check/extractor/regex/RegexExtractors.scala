/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.check.extractor.regex

import java.util.regex.{ Matcher, Pattern }

import scala.annotation.tailrec
import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.concurrent

import org.jboss.netty.util.internal.ConcurrentHashMap

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.check.Extractor
import io.gatling.core.check.extractor.Extractors.{ LiftedOption, LiftedSeqOption }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.StringHelper.ensureByteCopy
import io.gatling.core.validation.{ SuccessWrapper, Validation }

object GroupExtractor extends Logging {

	implicit val stringGroupExtractor = new GroupExtractor[String] {
		def extract(matcher: Matcher): String = {
			val value = matcher.group(matcher.groupCount min 1)
			ensureByteCopy(value)
		}
	}

	def safeGetGroupValue(matcher: Matcher, i: Int) =
		if (matcher.groupCount >= i)
			ensureByteCopy(matcher.group(i))
		else {
			logger.error(s"Regex group $i doesn't exist")
			""
		}

	implicit val stringTuple2GroupExtractor = new GroupExtractor[(String, String)] {
		def extract(matcher: Matcher): (String, String) = {
			val value1 = safeGetGroupValue(matcher, 1)
			val value2 = safeGetGroupValue(matcher, 2)
			(value1, value2)
		}
	}

	implicit val stringTuple3GroupExtractor = new GroupExtractor[(String, String, String)] {
		def extract(matcher: Matcher): (String, String, String) = {
			val value1 = safeGetGroupValue(matcher, 1)
			val value2 = safeGetGroupValue(matcher, 2)
			val value3 = safeGetGroupValue(matcher, 3)
			(value1, value2, value3)
		}
	}

	implicit val stringTuple4GroupExtractor = new GroupExtractor[(String, String, String, String)] {
		def extract(matcher: Matcher): (String, String, String, String) = {
			val value1 = safeGetGroupValue(matcher, 1)
			val value2 = safeGetGroupValue(matcher, 2)
			val value3 = safeGetGroupValue(matcher, 3)
			val value4 = safeGetGroupValue(matcher, 4)
			(value1, value2, value3, value4)
		}
	}
}

trait GroupExtractor[X] {
	def extract(matcher: Matcher): X
}

object RegexExtractors {

	val cache: concurrent.Map[String, Pattern] = new ConcurrentHashMap[String, Pattern]
	def cached(pattern: String) = if (configuration.core.extract.regex.cache) cache.getOrElseUpdate(pattern, Pattern.compile(pattern)) else Pattern.compile(pattern)

	abstract class RegexExtractor[X] extends Extractor[String, String, X] {
		val name = "regex"
	}

	implicit class RichMatcher(val matcher: Matcher) extends AnyVal {

		def foldLeft[T](zero: T)(f: (Matcher, T) => T): T = {
			var temp = zero
			while (matcher.find)
				temp = f(matcher, temp)
			temp
		}

		def findMatchN[X](n: Int)(implicit groupExtractor: GroupExtractor[X]): Option[X] = {

			@tailrec
			def findRec(countDown: Int): Boolean = {
				if (!matcher.find)
					false
				else if (countDown == 0)
					true
				else
					findRec(countDown - 1)
			}

			if (findRec(n))
				value[X].liftOption
			else
				None
		}

		def value[X](implicit groupExtractor: GroupExtractor[X]): X = groupExtractor.extract(matcher)
	}

	def extract[X](string: String, pattern: String)(implicit groupExtractor: GroupExtractor[X]): Seq[X] = {

		val matcher = cached(pattern).matcher(string)
		matcher.foldLeft(List.empty[X]) { (matcher, values) =>
			matcher.value :: values
		}.reverse
	}

	def extractOne[X](occurrence: Int)(implicit groupExtractor: GroupExtractor[X]) = new RegexExtractor[X] {

		def apply(prepared: String, criterion: String): Validation[Option[X]] = {
			val matcher = cached(criterion).matcher(prepared)
			matcher.findMatchN(occurrence).success
		}
	}

	def extractMultiple[X](implicit groupExtractor: GroupExtractor[X]) = new RegexExtractor[Seq[X]] {

		def apply(prepared: String, criterion: String): Validation[Option[Seq[X]]] = extract(prepared, criterion).liftSeqOption.success
	}

	val count = new RegexExtractor[Int] {

		def apply(prepared: String, criterion: String): Validation[Option[Int]] = {
			val matcher = cached(criterion).matcher(prepared)
			matcher.foldLeft(0) { (_, count) =>
				count + 1
			}.liftOption.success
		}
	}
}
