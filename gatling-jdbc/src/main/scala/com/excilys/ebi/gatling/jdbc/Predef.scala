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
import com.excilys.ebi.gatling.core.feeder.database.DatabaseFeederBuilder

object Predef {

	def databaseFeeder(driverClassName: String, url: String, username: String, password: String, sql: String) = DatabaseFeederBuilder.database(driverClassName, url, username, password, sql)
	def db2Feeder(url: String, username: String, password: String, sql: String) = DatabaseFeederBuilder.db2(url, username, password, sql)
	def hsqldbFeeder(url: String, username: String, password: String, sql: String) = DatabaseFeederBuilder.hsqldb(url, username, password, sql)
	def h2Feeder(url: String, username: String, password: String, sql: String) = DatabaseFeederBuilder.h2(url, username, password, sql)
	def mssqlFeeder(url: String, username: String, password: String, sql: String) = DatabaseFeederBuilder.mssql(url, username, password, sql)
	def mysqlFeeder(url: String, username: String, password: String, sql: String) = DatabaseFeederBuilder.mysql(url, username, password, sql)
	def oracleFeeder(url: String, username: String, password: String, sql: String) = DatabaseFeederBuilder.oracle(url, username, password, sql)
	def postgresqlFeeder(url: String, username: String, password: String, sql: String) = DatabaseFeederBuilder.postgresql(url, username, password, sql)
	def sybaseFeeder(url: String, username: String, password: String, sql: String) = DatabaseFeederBuilder.sybase(url, username, password, sql)

}