/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package com.excilys.ebi.gatling.jdbc

import java.sql.ResultSet

package object util {

	implicit class RowIterator(resultSet: ResultSet) extends Iterator[Array[AnyRef]] {

		val columnCount = resultSet.getMetaData.getColumnCount

		def hasNext = resultSet.next

		def next = (for (i <- 1 to columnCount) yield (resultSet.getObject(i))).toArray
	}

}
