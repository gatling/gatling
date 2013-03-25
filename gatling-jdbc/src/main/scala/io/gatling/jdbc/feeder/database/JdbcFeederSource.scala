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
package io.gatling.jdbc.feeder.database

import java.sql.DriverManager
import java.sql.ResultSet.{ CONCUR_READ_ONLY, TYPE_FORWARD_ONLY }

import scala.annotation.tailrec

import io.gatling.core.feeder.Record
import io.gatling.core.util.IOHelper.use

object JdbcFeederSource {

	def apply(url: String, username: String, password: String, sql: String): Array[Record[Any]] = {

		use(DriverManager.getConnection(url, username, password)) { connection =>
			val preparedStatement = connection.prepareStatement(sql, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY)
			val resultSet = preparedStatement.executeQuery
			val metadata = resultSet.getMetaData
			val columnCount = metadata.getColumnCount

			val columnNames = for (i <- 1 to columnCount) yield metadata.getColumnName(i)

			def computeRecord: Record[Any] = (for (i <- 1 to columnCount) yield (columnNames(i - 1) -> resultSet.getObject(i))).toMap

			@tailrec
			def loadRec(records: Vector[Record[Any]]): Vector[Record[Any]] =
				if (!resultSet.next) records
				else loadRec(records :+ computeRecord)

			loadRec(Vector.empty).toArray
		}
	}
}