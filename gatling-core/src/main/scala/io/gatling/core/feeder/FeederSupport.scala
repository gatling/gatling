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
package io.gatling.core.feeder

import scala.reflect.io.{ File, Path }

trait FeederSupport {

	type Feeder[T] = io.gatling.core.feeder.Feeder[T]

	def csv(fileName: String) = SeparatedValuesParser.csv(fileName)
	def csv(file: File) = SeparatedValuesParser.csv(file.path)
	def ssv(fileName: String) = SeparatedValuesParser.ssv(fileName)
	def ssv(file: File) = SeparatedValuesParser.ssv(file.path)
	def tsv(fileName: String) = SeparatedValuesParser.tsv(fileName)
	def tsv(file: File) = SeparatedValuesParser.tsv(file.path)

	implicit def array2FeederBuilder[T](data: Array[Map[String, T]]): AdvancedFeederBuilder[T] = AdvancedFeederBuilder(data)
	implicit def feeder2FeederBuilder[T](feeder: Feeder[T]): FeederBuilder[T] = FeederWrapper(feeder)
}
