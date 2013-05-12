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
import io.gatling.jdbc.config.JdbcProtocolConfigurationBuilder.{ PASSWORD, USER }
import io.gatling.jdbc.statement.action.actor.ConnectionFactory

import com.jolbox.bonecp.BoneCPDataSource

import com.typesafe.scalalogging.slf4j.Logging

object JdbcProtocolConfigurationBuilder {

	val USER = "user"
	val PASSWORD = "password"

	private[gatling] val BASE_JDBC_PROTOCOL_CONFIGURATION_BUILDER = new JdbcProtocolConfigurationBuilder(Attributes(properties = Map.empty))

	def jdbcConfig = BASE_JDBC_PROTOCOL_CONFIGURATION_BUILDER
}

private case class Attributes(
	url: String = "",
	driver: String = "",
	nbPartitions: Int = 1,
	size: Int = 1,
	defaultTransactionIsolation: Option[String] = None,
	defaultCatalog: Option[String] = None,
	defaultReadOnly: Option[java.lang.Boolean] = None,
	initSQL: Option[String] = None,
	properties: Map[String, Any])

// TODO : check size ? allow partitions size ?
class JdbcProtocolConfigurationBuilder(attributes: Attributes) extends Logging {

	def url(url: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(url = url))

	def driver(driver: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(driver = driver))

	def username(username: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(properties = attributes.properties + (USER -> username)))

	def password(password: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(properties = attributes.properties + (PASSWORD -> password)))

	def properties(properties: Map[String, Any]) = new JdbcProtocolConfigurationBuilder(attributes.copy(properties = properties))

	def partitions(nbPartitions: Int) = new JdbcProtocolConfigurationBuilder(attributes.copy(nbPartitions = nbPartitions))

	def size(size: Int) = new JdbcProtocolConfigurationBuilder(attributes.copy(size = size))

	def initSQL(sql: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(initSQL = Some(sql)))

	def defaultTransactionIsolation(isolationLevel: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(defaultTransactionIsolation = Some(isolationLevel)))

	def defaultReadOnly(readOnly: Boolean) = new JdbcProtocolConfigurationBuilder(attributes.copy(defaultReadOnly = Some(readOnly)))

	def defaultCatalog(catalog: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(defaultCatalog = Some(catalog)))

	private[jdbc] def build = {
		require(attributes.driver != "","JDBC driver is not configured.")
		require(attributes.url != "","JDBC connection URL is not configured.")
		require(attributes.properties.contains(USER), "username is not configured.")
		require(attributes.properties.contains(PASSWORD),"password is not configured.")

		ConnectionFactory.setDataSource(setupDataSource)
		system.registerOnTermination(ConnectionFactory.close)
		JdbcProtocolConfiguration
	}

	private def setupDataSource = {
		val ds = new BoneCPDataSource
		ds.setDriverClass(attributes.driver)
		ds.setJdbcUrl(attributes.url)
		ds.setDriverProperties(buildDriverProperties(attributes.properties))
		ds.setDefaultAutoCommit(true)
		ds.setPartitionCount(attributes.nbPartitions)
		ds.setMaxConnectionsPerPartition(attributes.size / attributes.nbPartitions)
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
