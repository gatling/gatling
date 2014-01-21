/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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

import scala.annotation.implicitNotFound

@implicitNotFound("Sorry, type inference was unable to figure out the type. You need to provide it explicitly.")
trait NotNothing[T]

object NotNothing {

	private val evidence: NotNothing[Any] = new Object with NotNothing[Any]

	implicit def notNothingEv[T](implicit n: T =:= T): NotNothing[T] = evidence.asInstanceOf[NotNothing[T]]
}
