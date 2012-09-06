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

import scala.collection.mutable

import com.twitter.scalding.{ AccessMode, Local, Mode, Source, Write }

import cascading.scheme.NullScheme
import cascading.tap.Tap
import cascading.tuple.{ Fields, TupleEntry }

class GatlingBufferSource[A](tupleBuffer: mutable.Buffer[A], parseFunction: (TupleEntry) => A, inFields: Fields = Fields.ALL) extends Source {

	override def createTap(readOrWrite: AccessMode)(implicit mode: Mode): Tap[_, _, _] = {
		(mode, readOrWrite) match {
			case (Local(_), Write) => new GatlingMemorySinkTap(new NullScheme(Fields.ALL, inFields), tupleBuffer, parseFunction)
			case _ => throw new UnsupportedOperationException
		}
	}
}
