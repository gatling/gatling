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
package io.gatling.recorder.config

import java.util.{ List => JList }

import scala.collection.mutable

import io.gatling.recorder.config.ConfigurationConstants._

class RecorderPropertiesBuilder {

	val props = mutable.Map.empty[String, Any]

	def filterStrategy(strategy: String) { props += FILTER_STRATEGY -> strategy }

	def patterns(patterns: JList[String]) { props += PATTERNS -> patterns }

	def patternsType(patternsType: JList[String]) { props += PATTERNS_TYPE -> patternsType }

	def automaticReferer(status: Boolean) { props += AUTOMATIC_REFERER -> status }

	def followRedirect(status: Boolean) { props += FOLLOW_REDIRECT -> status }

	def localPort(port: Int) { props += LOCAL_PORT -> port }

	def localSslPort(port: Int) { props += LOCAL_SSL_PORT -> port }

	def proxyHost(host: String) { props += PROXY_HOST -> host }

	def proxyUsername(username: String) { props += PROXY_USERNAME -> username }

	def proxyPassword(password: String) { props += PROXY_PASSWORD -> password }

	def proxyPort(port: Int) { props += PROXY_PORT -> port }

	def proxySslPort(port: Int) { props += PROXY_SSL_PORT -> port }

	def encoding(encoding: String) { props += ENCODING -> encoding }

	def simulationOutputFolder(folder: String) { props += SIMULATION_OUTPUT_FOLDER -> folder }

	def requestBodiesFolder(folder: String) { props += REQUEST_BODIES_FOLDER -> folder }

	def simulationPackage(pkg: String) { props += SIMULATION_PACKAGE -> pkg }

	def simulationClassName(className: String) { props += SIMULATION_CLASS_NAME -> className }

	def build = props
}