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
package com.excilys.ebi.gatling.core.session.handler

import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.session.Session
import com.excilys.ebi.gatling.core.action.Action

/**
 * This trait is used for mixin-composition, it is the top level trait
 *
 * The classes that will use this composition will have the iteration behavior that happen in three steps :
 *   init:      the initiation of the counter
 *   increment: the incrementation of the counter
 *   expire:    the release of the counter
 */
trait IterationHandler extends Logging {

	def init(session: Session, counterName: String) = session

	def increment(session: Session, counterName: String) = session

	def expire(session: Session, counterName: String) = session
}