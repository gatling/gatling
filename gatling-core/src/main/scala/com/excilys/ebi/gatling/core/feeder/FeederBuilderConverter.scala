/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.core.feeder

class FeederBuilderConverter[T](feederBuilder: AdvancedFeederBuilder[T], conversions: Seq[(String, T => Any)]) extends AdvancedFeederBuilder[Any] {

	override val strategy = feederBuilder.strategy

	lazy val data = {
		def convertColumn(column: String, content: T) = {
			val conversion = conversions.find(column == _._1).map(_._2).getOrElse(identity[T] _)
			conversion(content)
		}

		feederBuilder.data.map(_.map{ case (key,value) => (key,convertColumn(key,value))})
	}
}
