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
package com.excilys.ebi.gatling.core.util

import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime

/**
 * This object groups all utilities for dates
 */
object DateHelper {

	/**
	 * Formatter for human readable dates (logs)
	 */
	private val resultDateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")

	/**
	 * Formatter for folder dates
	 */
	private val fileNameDateTimeFormat = DateTimeFormat.forPattern("yyyyMMddHHmmss")

	/**
	 * Returns a date from a log string
	 *
	 * @param string the date as a string
	 * @return a date from a log string
	 */
	def parseResultDate(string: String) = DateTime.parse(string, resultDateTimeFormat);

	/**
	 * Returns a log string from a date
	 *
	 * @param dateTime the date to be formatted
	 * @return a log string from a date
	 */
	def printResultDate(dateTime: DateTime) = resultDateTimeFormat.print(dateTime)

	/**
	 * Returns a folder name from a date
	 *
	 * @param dateTime the date to be formatted
	 * @return a folder name from a date
	 */
	def printFileNameDate(dateTime: DateTime) = fileNameDateTimeFormat.print(dateTime)
}