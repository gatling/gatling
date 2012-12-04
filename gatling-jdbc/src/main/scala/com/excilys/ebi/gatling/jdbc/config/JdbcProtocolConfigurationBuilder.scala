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
package com.excilys.ebi.gatling.jdbc.config

import com.excilys.ebi.gatling.jdbc.config.JdbcProtocolConfigurationBuilder.{PASSWORD_KEY,USER_KEY}
import com.excilys.ebi.gatling.jdbc.util.ConnectionFactory
import grizzled.slf4j.Logging
import org.apache.tomcat.jdbc.pool.DataSource

object JdbcProtocolConfigurationBuilder {

	val USER_KEY = "user"
	val PASSWORD_KEY = "password"

	private[gatling] val BASE_JDBC_PROTOCOL_CONFIGURATION_BUILDER =
		new JdbcProtocolConfigurationBuilder(Attributes("",None,None,None,None,None,None,None,None,None,Map[String, Any]()))

	def jdbcConfig = BASE_JDBC_PROTOCOL_CONFIGURATION_BUILDER
}

private case class Attributes(
	url: String,
	initial: Option[Int],
	minIdle: Option[Int],
	maxActive: Option[Int],
	maxIdle: Option[Int],
	maxWait: Option[Int],
	defaultTransactionIsolation: Option[Int],
	defaultCatalog: Option[String],
	defaultReadOnly: Option[java.lang.Boolean],
	initSQL: Option[String],
	properties: Map[String, Any])

class JdbcProtocolConfigurationBuilder(attributes: Attributes) extends Logging {

	def url(url: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(url = url))

	def username(username: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(properties = attributes.properties + (USER_KEY -> username)))

	def password(password: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(properties = attributes.properties + (PASSWORD_KEY -> password)))

	def properties(properties: Map[String, Any]) = new JdbcProtocolConfigurationBuilder(attributes.copy(properties = properties))

	def initial(initial: Int) = new JdbcProtocolConfigurationBuilder(attributes.copy(initial = Some(initial)))

	def minIdle(minIdle: Int) = new JdbcProtocolConfigurationBuilder(attributes.copy(minIdle = Some(minIdle)))

	def maxActive(maxActive: Int) = new JdbcProtocolConfigurationBuilder(attributes.copy(maxActive = Some(maxActive)))

	def maxIdle(maxIdle: Int) = new JdbcProtocolConfigurationBuilder(attributes.copy(maxIdle = Some(maxIdle)))

	def maxWait(maxWait: Int) = new JdbcProtocolConfigurationBuilder(attributes.copy(maxWait = Some(maxWait)))

	def initSQL(sql: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(initSQL = Some(sql)))

	def defaultTransactionIsolation(isolationLevel: Int) = new JdbcProtocolConfigurationBuilder(attributes.copy(defaultTransactionIsolation = Some(isolationLevel)))

	def defaultReadOnly(readOnly: Boolean) = new JdbcProtocolConfigurationBuilder(attributes.copy(defaultReadOnly = Some(readOnly)))

	def defaultCatalog(catalog: String) = new JdbcProtocolConfigurationBuilder(attributes.copy(defaultCatalog = Some(catalog)))

	private[jdbc] def build = {
		if(attributes.url == "")
			throw new IllegalArgumentException("JDBC connection URL is not defined.")
		if(!attributes.properties.contains(USER_KEY))
			throw new IllegalArgumentException("User is not defined.")
		if(!attributes.properties.contains(PASSWORD_KEY))
			throw new IllegalArgumentException("Password is not defined.")
		ConnectionFactory.setDataSource(setupDataSource)
		JdbcProtocolConfiguration
	}

	def setupDataSource: DataSource = {
		val ds = new DataSource
		ds.setUrl(attributes.url)
		ds.setConnectionProperties(buildPropertiesString(attributes.properties))
		ds.setDefaultAutoCommit(true)
		callIfSome(attributes.initial,ds.setInitialSize)
		callIfSome(attributes.minIdle,ds.setMinIdle)
		callIfSome(attributes.maxActive,ds.setMaxActive)
		callIfSome(attributes.maxIdle,ds.setMaxIdle)
		callIfSome(attributes.maxWait,ds.setMaxWait)
		callIfSome(attributes.initSQL,ds.setInitSQL)
		callIfSome(attributes.defaultTransactionIsolation,ds.setDefaultTransactionIsolation)
		callIfSome(attributes.defaultReadOnly,ds.setDefaultReadOnly)
		callIfSome(attributes.defaultCatalog,ds.setDefaultCatalog)
		ds
	}

	private def buildPropertiesString(map: Map[String, Any]) = map.map { case (key, value) => key + "=" + value }.mkString(";")

	private def callIfSome[T <: Any](value: Option[T],f: T => Unit) = if(value.isDefined) f(value.get)
}
