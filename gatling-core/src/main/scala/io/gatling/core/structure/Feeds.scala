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
package io.gatling.core.structure

import io.gatling.core.action.builder.FeedBuilder
import io.gatling.core.feeder.FeederBuilder
import io.gatling.core.session.{ Expression, ExpressionWrapper }

object Feeds {
	val oneExpression = 1.expression
}

trait Feeds[B] extends Execs[B] {

	/**
	 * Method used to load data from a feeder in the current scenario
	 *
	 * @param feederBuilder the feeder from which the values will be loaded
	 * @param number the number of records to be polled (default 1)
	 */
	def feed(feederBuilder: FeederBuilder[_], number: Expression[Int] = Feeds.oneExpression): B = exec(FeedBuilder(feederBuilder, number))
}
