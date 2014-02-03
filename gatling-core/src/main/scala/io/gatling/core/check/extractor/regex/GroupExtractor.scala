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
package io.gatling.core.check.extractor.regex

import scala.annotation.implicitNotFound

import java.util.regex.Matcher

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.util.StringHelper.RichString

trait LowPriorityGroupExtractorImplicits extends StrictLogging {

	implicit val stringGroupExtractor = new GroupExtractor[String] {
		def extract(matcher: Matcher): String =
			matcher.group(matcher.groupCount min 1).ensureTrimmedCharsArray
	}

	def safeGetGroupValue(matcher: Matcher, i: Int) =
		if (matcher.groupCount >= i)
			matcher.group(i).ensureTrimmedCharsArray
		else {
			logger.error(s"Regex group $i doesn't exist")
			""
		}

	implicit val groupExtractor2 = new GroupExtractor[(String, String)] {
		def extract(matcher: Matcher) = (
			safeGetGroupValue(matcher, 1),
			safeGetGroupValue(matcher, 2))
	}

	implicit val groupExtractor3 = new GroupExtractor[(String, String, String)] {
		def extract(matcher: Matcher) = (
			safeGetGroupValue(matcher, 1),
			safeGetGroupValue(matcher, 2),
			safeGetGroupValue(matcher, 3))
	}

	implicit val groupExtractor4 = new GroupExtractor[(String, String, String, String)] {
		def extract(matcher: Matcher) = (
			safeGetGroupValue(matcher, 1),
			safeGetGroupValue(matcher, 2),
			safeGetGroupValue(matcher, 3),
			safeGetGroupValue(matcher, 4))
	}

	implicit val groupExtractor5 = new GroupExtractor[(String, String, String, String, String)] {
		def extract(matcher: Matcher) = (
			safeGetGroupValue(matcher, 1),
			safeGetGroupValue(matcher, 2),
			safeGetGroupValue(matcher, 3),
			safeGetGroupValue(matcher, 4),
			safeGetGroupValue(matcher, 5))
	}

	implicit val groupExtractor6 = new GroupExtractor[(String, String, String, String, String, String)] {
		def extract(matcher: Matcher) = (
			safeGetGroupValue(matcher, 1),
			safeGetGroupValue(matcher, 2),
			safeGetGroupValue(matcher, 3),
			safeGetGroupValue(matcher, 4),
			safeGetGroupValue(matcher, 5),
			safeGetGroupValue(matcher, 6))
	}

	implicit val groupExtractor7 = new GroupExtractor[(String, String, String, String, String, String, String)] {
		def extract(matcher: Matcher) = (
			safeGetGroupValue(matcher, 1),
			safeGetGroupValue(matcher, 2),
			safeGetGroupValue(matcher, 3),
			safeGetGroupValue(matcher, 4),
			safeGetGroupValue(matcher, 5),
			safeGetGroupValue(matcher, 6),
			safeGetGroupValue(matcher, 7))
	}

	implicit val groupExtractor8 = new GroupExtractor[(String, String, String, String, String, String, String, String)] {
		def extract(matcher: Matcher) = (
			safeGetGroupValue(matcher, 1),
			safeGetGroupValue(matcher, 2),
			safeGetGroupValue(matcher, 3),
			safeGetGroupValue(matcher, 4),
			safeGetGroupValue(matcher, 5),
			safeGetGroupValue(matcher, 6),
			safeGetGroupValue(matcher, 7),
			safeGetGroupValue(matcher, 8))
	}
}

object GroupExtractor extends LowPriorityGroupExtractorImplicits

@implicitNotFound(msg = "Cannot find GroupExtractor type class for type ${X}")
trait GroupExtractor[X] {
	def extract(matcher: Matcher): X
}
