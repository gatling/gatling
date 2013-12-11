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

import io.gatling.core.session.Session
import io.gatling.core.validation.{ SuccessWrapper, Validation, noneSuccess }

package object session {

	type Expression[T] = Session => Validation[T]

	implicit class ExpressionWrapper[T](val value: T) extends AnyVal {
		def expression: Expression[T] = {
			val valueS = value.success
			_ => valueS
		}
	}

	implicit class RichExpression[T](val expression: Expression[T]) extends AnyVal {
		def map[T2](f: T => T2): Expression[T2] = session => expression(session).map(f)
	}

	val noopStringExpression = "".expression

	def resolveOptionalExpression[T](expression: Option[Expression[T]], session: Session): Validation[Option[T]] = expression match {
		case Some(e) => e(session).map(Some.apply)
		case _ => noneSuccess
	}
}
