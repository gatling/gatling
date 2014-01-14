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
package io.gatling.core.structure

import io.gatling.core.action.builder.GroupBuilder
import io.gatling.core.session.Expression
import io.gatling.core.structure.ChainBuilder.chainOf

trait Groups[B] extends Execs[B] {

	def group(name: Expression[String])(chain: ChainBuilder): B = exec(chainOf(GroupBuilder.start(name)).exec(chain).exec(GroupBuilder.end))
}