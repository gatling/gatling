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

import com.excilys.ebi.gatling.core.config.GatlingFiles.GATLING_HOME

import akka.config.{ ResourceImporter, Importer, FilesystemImporter, ConfigurationException, ConfigParser }
import grizzled.slf4j.Logging

object GatlingFileConfiguration extends Logging {
	val defaultPath = new File(GATLING_HOME).getCanonicalPath
	lazy val filesystemImporter = new FilesystemImporter(defaultPath)
	lazy val resourceImporter = new ResourceImporter(getClass.getClassLoader)

	private def load(data: String, givenImporter: Importer) = new GatlingFileConfiguration(new ConfigParser(importer = givenImporter).parse(data))

	private def fromFile(filename: String, importer: Importer) = load(importer.importFile(filename), importer)

	def fromFile(filename: String): GatlingFileConfiguration = {

		if (new File(filename).exists || new File(defaultPath, filename).exists) {
			info("loading conf file from filesystem " + filename)
			fromFile(filename, filesystemImporter)

		} else {
			info("loading conf file from classpath " + filename)
			fromFile(filename, resourceImporter)
		}
	}
}

class GatlingFileConfiguration(map: Map[String, Any]) extends Logging {

	def contains(key: String): Boolean = map contains key

	def keys: Iterable[String] = map.keys

	def getAny(key: String): Option[Any] = map.get(key)

	def getAny(key: String, defaultValue: Any): Any = getAny(key).getOrElse(defaultValue)

	def getSeqAny(key: String): Seq[Any] = map
		.get(key)
		.getOrElse {
			debug(key + " config is not defined")
			Seq.empty[Any]
		}.asInstanceOf[Seq[Any]]

	def getString(key: String): Option[String] = map.get(key).map(_.toString)

	def getString(key: String, defaultValue: String): String = getString(key).getOrElse(defaultValue)

	def getList(key: String): Seq[String] = map
		.get(key)
		.getOrElse {
			debug(key + " config is not defined")
			Seq.empty[String]
		}.asInstanceOf[Seq[String]]

	def getInt(key: String): Option[Int] = getString(key).map(_.toInt)

	def getInt(key: String, defaultValue: Int): Int = getInt(key).getOrElse(defaultValue)

	def getLong(key: String): Option[Long] = getString(key).map(_.toLong)

	def getLong(key: String, defaultValue: Long): Long = getLong(key).getOrElse(defaultValue)

	def getFloat(key: String): Option[Float] = getString(key).map(_.toFloat)

	def getFloat(key: String, defaultValue: Float): Float = getFloat(key).getOrElse(defaultValue)

	def getDouble(key: String): Option[Double] = getString(key).map(_.toDouble)

	def getDouble(key: String, defaultValue: Double): Double = getDouble(key).getOrElse(defaultValue)

	def getBoolean(key: String): Option[Boolean] = getString(key).map(_.toBoolean)

	def getBoolean(key: String, defaultValue: Boolean): Boolean = getBoolean(key).getOrElse(defaultValue)

	def apply(key: String): String = getString(key).getOrElse(throw new ConfigurationException("undefined config: " + key))

	def apply(key: String, defaultValue: String) = getString(key, defaultValue)
	def apply(key: String, defaultValue: Int) = getInt(key, defaultValue)
	def apply(key: String, defaultValue: Long) = getLong(key, defaultValue)
	def apply(key: String, defaultValue: Boolean) = getBoolean(key, defaultValue)
}
