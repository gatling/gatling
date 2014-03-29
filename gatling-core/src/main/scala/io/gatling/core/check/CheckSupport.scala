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
package io.gatling.core.check

trait CheckSupport {

  implicit def checkBuilder2Check[C <: Check[R], R, P, X](checkBuilder: CheckBuilder[C, R, P, X]) = checkBuilder.build
  implicit def validatorCheckBuilder2CheckBuilder[C <: Check[R], R, P, X](validatorCheckBuilder: ValidatorCheckBuilder[C, R, P, X]) = validatorCheckBuilder.exists
  implicit def validatorCheckBuilder2Check[C <: Check[R], R, P, X](validatorCheckBuilder: ValidatorCheckBuilder[C, R, P, X]) = validatorCheckBuilder.exists.build
  implicit def findCheckBuilder2ValidatorCheckBuilder[C <: Check[R], R, P, X](findCheckBuilder: FindCheckBuilder[C, R, P, X]) = findCheckBuilder.find
  implicit def findCheckBuilder2CheckBuilder[C <: Check[R], R, P, X](findCheckBuilder: FindCheckBuilder[C, R, P, X]) = findCheckBuilder.find.exists
  implicit def findCheckBuilder2Check[C <: Check[R], R, P, X](findCheckBuilder: FindCheckBuilder[C, R, P, X]) = findCheckBuilder.find.exists.build
}
