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
package io.gatling.core.util

import java.io.{ File => JFile }
import java.net.{ URISyntaxException, URL }
import java.security.MessageDigest

import scala.util.Try

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.util.StringHelper.{ RichString, bytes2Hex }

/**
 * This object groups all utilities for files
 */
object FileHelper {

	val commaSeparator = ","
	val semicolonSeparator = ";"
	val tabulationSeparator = "\t"

	implicit class FileRichString(val string: String) extends AnyVal {

		/**
		 * Transform a string to a simpler one that can be used safely as file name
		 *
		 * @param s the string to be simplified
		 * @return a simplified string
		 */
		def toFileName = {

			val trimmed = string.trim match {
				case "" => "missing_name"
				case s => s
			}

			val md = MessageDigest.getInstance("md5")
			md.update(trimmed.getBytes(configuration.core.charSet))
			trimmed.clean + "-" + bytes2Hex(md.digest)
		}

		def toRequestFileName = s"req_${string.toFileName}.html"
	}

	implicit class RichURL(val url: URL) extends AnyVal {

		def jfile(): JFile = Try(new JFile(url.toURI))
			.recover { case e: URISyntaxException => new JFile(url.getPath) }
			.get
	}
}
