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
package io.gatling.core

import scala.reflect.ClassTag

import io.gatling.core.validation.{ SuccessWrapper, Validation }

package object session {

	implicit class ELCompiler(val string: String) extends AnyVal {
		def el[T: ClassTag] = EL.compile(string)
	}

	implicit class ELWrapper[T](val value: T) extends AnyVal {
		def expression = (session: Session) => value.success
	}

	val noopStringExpression = "".expression

	type Expression[T] = Session => Validation[T]
	def undefinedSeqIndexMessage(name: String, index: Int) = s"Seq named '$name' is undefined for index $index"
	def undefinedSessionAttributeMessage(name: String) = s"No attribute named '$name' is defined"
	def resolveOptionalExpression[T](expression: Option[Expression[T]], session: Session): Validation[Option[T]] = expression.map(_(session).map(Some(_))).getOrElse(None.success)
}
