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
package io.gatling.core.feeder

import scala.reflect.io.File

import io.gatling.core.config.Resource
import io.gatling.core.util.FileHelper.{ commaSeparator, semicolonSeparator, tabulationSeparator }
import io.gatling.core.validation.{ Failure, Success, Validation }

trait FeederSupport {

	type Feeder[T] = io.gatling.core.feeder.Feeder[T]

	implicit def array2FeederBuilder[T](data: Array[Map[String, T]]): RecordArrayFeederBuilder[T] = RecordArrayFeederBuilder(data)
	implicit def feeder2FeederBuilder[T](feeder: Feeder[T]): FeederBuilder[T] = FeederWrapper(feeder)

	def csv(file: File): RecordArrayFeederBuilder[String] = csv(file.path)
	def csv(fileName: String): RecordArrayFeederBuilder[String] = separatedValues(fileName, commaSeparator.charAt(0))
	def ssv(file: File): RecordArrayFeederBuilder[String] = ssv(file.path)
	def ssv(fileName: String): RecordArrayFeederBuilder[String] = separatedValues(fileName, semicolonSeparator.charAt(0))
	def tsv(file: File): RecordArrayFeederBuilder[String] = tsv(file.path)
	def tsv(fileName: String): RecordArrayFeederBuilder[String] = separatedValues(fileName, tabulationSeparator.charAt(0))

	def separatedValues(fileName: String, separator: Char, quoteChar: Char = '"'): RecordArrayFeederBuilder[String] = separatedValues(Resource.feeder(fileName), separator, quoteChar)

	def separatedValues(resource: Validation[Resource], separator: Char, quoteChar: Char): RecordArrayFeederBuilder[String] = resource match {
		case Success(resource) => RecordArrayFeederBuilder(SeparatedValuesParser.parse(resource, separator, quoteChar))

		case Failure(message) => throw new IllegalArgumentException(s"Could not locate feeder file; $message")
	}
}
