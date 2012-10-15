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
package com.excilys.ebi.gatling.core.structure

import com.excilys.ebi.gatling.core.action.builder.{ ActionBuilder, BypassSimpleActionBuilder }
import com.excilys.ebi.gatling.core.session.Session

trait Execs[B] {

	private[core] def actionBuilders: List[ActionBuilder]
	private[core] def newInstance(actionBuilders: List[ActionBuilder]): B
	private[core] def getInstance: B

	/**
	 * Method used to execute an action
	 *
	 * @param actionBuilder the action builder representing the action to be executed
	 */
	def exec(sessionFunction: Session => Session): B = exec(BypassSimpleActionBuilder(sessionFunction))
	def exec(actionBuilder: ActionBuilder): B = newInstance(actionBuilder :: actionBuilders)
	def exec(chains: ChainBuilder*): B = exec(chains.toIterable)
	def exec(chains: Iterator[ChainBuilder]): B = exec(chains.toIterable)
	def exec(chains: Iterable[ChainBuilder]): B = newInstance(chains.toList.reverse.map(_.actionBuilders).flatten ::: actionBuilders)
}