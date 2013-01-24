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
package com.excilys.ebi.gatling.core.check.extractor

trait Extractor {

	/**
	 * @param value
	 * @return the value as an option
	 */
	implicit def toOption[X](value: X): Option[X] = Some(value)

	/**
	 * @param values
	 * @return the values as an option if they are not empty, elseNone
	 */
	implicit def seqToOption[X](values: Seq[X]): Option[Seq[X]] = if (values.isEmpty) None else Some(values)

}
