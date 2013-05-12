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
package io.gatling.jdbc.statement.action.actor

import com.jolbox.bonecp.BoneCPDataSource
import java.sql.SQLException


object ConnectionFactory {

	private var dataSource : BoneCPDataSource = _

	private[jdbc] def setDataSource(ds: BoneCPDataSource) { dataSource = ds }

	private[jdbc] def getConnection =
		if(dataSource != null)
			dataSource.getConnection
		else
			throw new SQLException("DataSource is not configured.")

	private[jdbc] def close = if (dataSource != null ) dataSource.close
}
