/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.charts.result.reader.scalding

import com.twitter.scalding.{ AccessMode, Local, Mode, Read, Source }

import cascading.scheme.NullScheme
import cascading.tap.Tap
import cascading.tuple.Fields

case class GatlingInputIteratorSource(inputIterator: Iterator[String], fields: Fields, size: Long) extends Source {
	override def createTap(readOrWrite: AccessMode)(implicit mode: Mode): Tap[_, _, _] = {
		(mode, readOrWrite) match {
			case (Local(_), Read) => new GatlingInputIteratorTap(inputIterator, new NullScheme(fields, Fields.ALL), size)
			case _ => throw new UnsupportedOperationException
		}
	}
}
