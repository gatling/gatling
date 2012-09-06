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

import cascading.scheme.NullScheme
import java.util.{ UUID, Properties }
import collection.mutable
import cascading.tuple.{ TupleEntry, TupleEntryCollector }
import cascading.tap.SinkTap
import cascading.flow.FlowProcess

class GatlingMemorySinkTap[A](val scheme: NullScheme[Properties, Void, Void, Void, Void], val tupleBuffer: mutable.Buffer[A], val parseFunction: (TupleEntry) => A)
	extends SinkTap[Properties, Void](scheme) {

	override def createResource(conf: Properties): Boolean = true

	override def deleteResource(conf: Properties): Boolean = true

	override def resourceExists(conf: Properties): Boolean = true

	override def getModifiedTime(conf: Properties): Long = 1L

	override val getIdentifier: String = classOf[GatlingMemorySinkTap[A]].getCanonicalName + UUID.randomUUID

	override def openForWrite(flowProcess: FlowProcess[Properties], output: Void): TupleEntryCollector = new GatlingMemoryTupleEntryCollector(tupleBuffer, parseFunction)

	override def equals(other: Any): Boolean = this.eq(other.asInstanceOf[AnyRef])

	override def hashCode: Int = System.identityHashCode(this)

}

class GatlingMemoryTupleEntryCollector[A](val tupleBuffer: mutable.Buffer[A], val parseFunction: (TupleEntry) => A) extends TupleEntryCollector {

	override def collect(tupleEntry: TupleEntry) {
		tupleBuffer += parseFunction(tupleEntry)
	}
}
