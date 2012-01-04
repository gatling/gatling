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
package com.excilys.ebi.gatling.core.feeder.database
import com.excilys.ebi.gatling.core.feeder.FeederBuilder

object DatabaseFeederBuilder{
	
	def database(driverClassName: String, url: String, username: String, password: String, sql: String) = new DatabaseFeederBuilder(driverClassName, url, username, password, sql)
	def db2(url: String, username: String, password: String, sql: String) = new DatabaseFeederBuilder("com.ibm.db2.jdbc.app.DB2Driver", url, username, password, sql)
	def hsqldb(url: String, username: String, password: String, sql: String) = new DatabaseFeederBuilder("org.hsql.jdbcDriver", url, username, password, sql)
	def h2(url: String, username: String, password: String, sql: String) = new DatabaseFeederBuilder("org.h2.Driver", url, username, password, sql)
	def mssql(url: String, username: String, password: String, sql: String) = new DatabaseFeederBuilder("com.microsoft.sqlserver.jdbc.SQLServerDriver", url, username, password, sql)
	def mysql(url: String, username: String, password: String, sql: String) = new DatabaseFeederBuilder("org.gjt.mm.mysql.Driver", url, username, password, sql)
	def oracle(url: String, username: String, password: String, sql: String) = new DatabaseFeederBuilder("oracle.jdbc.driver.OracleDriver", url, username, password, sql)
	def postgresql(url: String, username: String, password: String, sql: String) = new DatabaseFeederBuilder("org.postgresql.Driver", url, username, password, sql)
	def sybase(url: String, username: String, password: String, sql: String) = new DatabaseFeederBuilder("com.sybase.jdbc2.jdbc.SybDriver", url, username, password, sql)
}

class DatabaseFeederBuilder(driverClassName: String, url: String, username: String, password: String, sql: String) extends FeederBuilder[DatabaseFeederSource] {
	def sourceInstance = new DatabaseFeederSource(driverClassName, url, username, password, sql)
}