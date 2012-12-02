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
		new JdbcProtocolConfigurationBuilder(Attributes("",None,None,None,None,None,None,Map[String, Any]()))

	def jdbcConfig = BASE_JDBC_PROTOCOL_CONFIGURATION_BUILDER
}

private case class Attributes(
	url: String,
	initial: Option[Int],
	minIdle: Option[Int],
	maxActive: Option[Int],
	maxIdle: Option[Int],
	maxWait: Option[Int],
	initSQL: Option[String],
	properties: Map[String, Any])

class JdbcProtocolConfigurationBuilder(attributes: Attributes) extends Logging {

	val ds: DataSource = new DataSource

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

	def defaultTransactionIsolation(isolationLevel: Int) = {
		ds.setDefaultTransactionIsolation(isolationLevel)
		this
	}

	def defaultCatalog(catalog: String) = {
		ds.setDefaultCatalog(catalog)
		this
	}

	def abandonWhenPercentageFull(percentage: Int) = {
		ds.setAbandonWhenPercentageFull(percentage)
		this
	}

	def minEvictableIdleTimeMillis(minTime: Int) = {
		ds.setMinEvictableIdleTimeMillis(minTime)
		this
	}

	def timeBetweenEvictionRunsMillis(sleepTime: Int) = {
		ds.setTimeBetweenEvictionRunsMillis(sleepTime)
		this
	}

	def validationInterval(interval: Long) = {
		ds.setValidationInterval(interval)
		this
	}

	def validationQuery(query: String) = {
		ds.setValidationQuery(query)
		this
	}

	def defaultReadOnly(state: Boolean) = {
		ds.setDefaultReadOnly(state)
		this
	}

	def commitOnReturn(state: Boolean) = {
		ds.setCommitOnReturn(state)
		this
	}

	def rollbackOnReturn(state: Boolean) = {
		ds.setRollbackOnReturn(state)
		this
	}

	def fairQueue(state: Boolean) = {
		ds.setFairQueue(state)
		this
	}

	def testOnBorrow(state: Boolean) = {
		ds.setTestOnBorrow(state)
		this
	}

	def testOnConnect(state: Boolean) = {
		ds.setTestOnConnect(state)
		this
	}

	def testOnReturn(state: Boolean) = {
		ds.setTestOnReturn(state)
		this
	}

	def testWhileIdle(state: Boolean) = {
		ds.setTestWhileIdle(state)
		this
	}

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
		ds.setUrl(attributes.url)
		ds.setConnectionProperties(buildPropertiesString(attributes.properties))
		ds.setDefaultAutoCommit(true)
		callIfSome(attributes.initial,ds.setInitialSize)
		callIfSome(attributes.minIdle,ds.setMinIdle)
		callIfSome(attributes.maxActive,ds.setMaxActive)
		callIfSome(attributes.maxIdle,ds.setMaxIdle)
		callIfSome(attributes.maxWait,ds.setMaxWait)
		callIfSome(attributes.initSQL,ds.setInitSQL)
		ds
	}

	private def buildPropertiesString(map: Map[String, Any]) = map.map {case (key, value) => key + "=" + value}.mkString(";")

	private def callIfSome[T <: Any](value: Option[T],f: T => Unit) = if(value.isDefined) f(value.get)
}
