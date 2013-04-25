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

import java.io.Closeable
import java.sql.Connection

import scala.io.Source

object IOHelper {

	def withCloseable[T, C <: Closeable](closeable: C)(block: C => T) = {
		try
			block(closeable)
		finally
			closeable.close
	}

	def withSource[T, C <: Source](closeable: C)(block: C => T) = {
		try
			block(closeable)
		finally
			closeable.close
	}

	def withConnection[T, C <: Connection](closeable: C)(block: C => T) = {
		try
			block(closeable)
		finally
			closeable.close
	}
}