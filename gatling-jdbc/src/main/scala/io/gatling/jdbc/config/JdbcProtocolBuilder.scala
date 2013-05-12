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
package io.gatling.jdbc.config

import java.util.Properties

import io.gatling.core.action.system
import io.gatling.jdbc.config.JdbcProtocolBuilder.{ PASSWORD, USER }
import io.gatling.jdbc.statement.action.actor.ConnectionFactory

import com.jolbox.bonecp.BoneCPDataSource

import com.typesafe.scalalogging.slf4j.Logging

object JdbcProtocolBuilder {

	val USER = "user"
	val PASSWORD = "password"

	private[gatling] val BASE_JDBC_PROTOCOL_CONFIGURATION_BUILDER = new JdbcProtocolBuilder(Attributes(properties = Map.empty))

	def jdbcConfig = BASE_JDBC_PROTOCOL_CONFIGURATION_BUILDER
}

private case class Attributes(
	url: String = "",
	driver: String = "",
	nbPartitions: Int = 1,
	maxConnectionsPerPartition: Int = 1,
	defaultTransactionIsolation: Option[String] = None,
	defaultCatalog: Option[String] = None,
	defaultReadOnly: Option[java.lang.Boolean] = None,
	initSQL: Option[String] = None,
	properties: Map[String, Any])

// TODO : check size ? allow partitions size ?
class JdbcProtocolBuilder(attributes: Attributes) extends Logging {

	def url(url: String) = new JdbcProtocolBuilder(attributes.copy(url = url))

	def driver(driver: String) = new JdbcProtocolBuilder(attributes.copy(driver = driver))

	def username(username: String) = new JdbcProtocolBuilder(attributes.copy(properties = attributes.properties + (USER -> username)))

	def password(password: String) = new JdbcProtocolBuilder(attributes.copy(properties = attributes.properties + (PASSWORD -> password)))

	def properties(properties: Map[String, Any]) = new JdbcProtocolBuilder(attributes.copy(properties = properties))

	def partitions(nbPartitions: Int) = new JdbcProtocolBuilder(attributes.copy(nbPartitions = nbPartitions))

	def maxConnectionsPerPartition(maxConnectionsPerPartition: Int) = new JdbcProtocolBuilder(attributes.copy(maxConnectionsPerPartition = maxConnectionsPerPartition))

	def initSQL(sql: String) = new JdbcProtocolBuilder(attributes.copy(initSQL = Some(sql)))

	def defaultTransactionIsolation(isolationLevel: String) = new JdbcProtocolBuilder(attributes.copy(defaultTransactionIsolation = Some(isolationLevel)))

	def defaultReadOnly(readOnly: Boolean) = new JdbcProtocolBuilder(attributes.copy(defaultReadOnly = Some(readOnly)))

	def defaultCatalog(catalog: String) = new JdbcProtocolBuilder(attributes.copy(defaultCatalog = Some(catalog)))

	private[jdbc] def build = {
		require(attributes.driver != "","JDBC driver is not configured.")
		require(attributes.url != "","JDBC connection URL is not configured.")
		require(attributes.properties.contains(USER), "username is not configured.")
		require(attributes.properties.contains(PASSWORD),"password is not configured.")

		ConnectionFactory.setDataSource(setupDataSource)
		system.registerOnTermination(ConnectionFactory.close)
		JdbcProtocol
	}

	private def setupDataSource = {
		val ds = new BoneCPDataSource
		ds.setDriverClass(attributes.driver)
		ds.setJdbcUrl(attributes.url)
		ds.setDriverProperties(buildDriverProperties(attributes.properties))
		ds.setDefaultAutoCommit(true)
		ds.setPartitionCount(attributes.nbPartitions)
		ds.setMaxConnectionsPerPartition(attributes.maxConnectionsPerPartition)
		attributes.defaultReadOnly.foreach(ds.setDefaultReadOnly)
		attributes.defaultCatalog.foreach(ds.setDefaultCatalog)
		attributes.initSQL.foreach(ds.setInitSQL)
		attributes.defaultTransactionIsolation.foreach(ds.setDefaultTransactionIsolation)
		ds
	}

	private def buildDriverProperties(map: Map[String,Any]) = {
		val props = new Properties
		for ((key,value) <- map) props.put(key,value.toString)
		props
	}

}
