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
package com.excilys.ebi.gatling.core.config

import java.io.File

import scala.collection.JavaConversions.asScalaBuffer

import com.excilys.ebi.gatling.core.config.GatlingFiles.GATLING_HOME
import com.typesafe.config.{ ConfigFactory, Config }

import akka.config.ConfigurationException
import grizzled.slf4j.Logging

object GatlingFileConfiguration extends Logging {

	val defaultPath = new File(GATLING_HOME).getCanonicalPath

	private def fromFileSystem(file: File) = ConfigFactory.parseFile(file)

	private def fromClasspath(filename: String) = ConfigFactory.parseResources(getClass.getClassLoader, filename)

	def fromFile(filename: String): GatlingFileConfiguration = {

		val config = {
			val absoluteFile = new File(filename)
			if (absoluteFile.exists)
				fromFileSystem(absoluteFile)
			else {
				val relativeFile = new File(defaultPath, filename)
				if (relativeFile.exists)
					fromFileSystem(relativeFile)
				else
					fromClasspath(filename)
			}
		}

		new GatlingFileConfiguration(config)
	}
}

class GatlingFileConfiguration(map: Config) {

	def contains(key: String): Boolean = map.hasPath(key)

	def getString(key: String): Option[String] = if (contains(key)) Some(map.getString(key)) else None
	def getString(key: String, defaultValue: String): String = getString(key).getOrElse(defaultValue)

	def getList(key: String): Seq[String] = if (contains(key)) map.getStringList(key) else List.empty

	def getInt(key: String): Option[Int] = if (contains(key)) Some(map.getInt(key)) else None
	def getInt(key: String, defaultValue: Int): Int = getInt(key).getOrElse(defaultValue)

	def getLong(key: String): Option[Long] = if (contains(key)) Some(map.getLong(key)) else None
	def getLong(key: String, defaultValue: Long): Long = getLong(key).getOrElse(defaultValue)

	def getDouble(key: String): Option[Double] = if (contains(key)) Some(map.getDouble(key)) else None
	def getDouble(key: String, defaultValue: Double): Double = getDouble(key).getOrElse(defaultValue)

	def getBoolean(key: String): Option[Boolean] = if (contains(key)) Some(map.getBoolean(key)) else None
	def getBoolean(key: String, defaultValue: Boolean): Boolean = getBoolean(key).getOrElse(defaultValue)

	def apply(key: String): String = getString(key).getOrElse(throw new ConfigurationException("undefined config: " + key))
	def apply(key: String, defaultValue: String) = getString(key, defaultValue)
	def apply(key: String, defaultValue: Int) = getInt(key, defaultValue)
	def apply(key: String, defaultValue: Long) = getLong(key, defaultValue)
	def apply(key: String, defaultValue: Double) = getDouble(key, defaultValue)
	def apply(key: String, defaultValue: Boolean) = getBoolean(key, defaultValue)
}
