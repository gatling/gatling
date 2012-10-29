/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.metrics.types

import com.excilys.ebi.gatling.core.result.message.{ ActionType, ScenarioRecord }

class UserMetric(val nbUsers: Int) {

	private var _active = 0
	private var activeBuffer = 0
	private var _waiting = nbUsers
	private var _done = 0
	private var doneBuffer = 0

	def update(scenarioRecord: ScenarioRecord) {
		scenarioRecord.actionType match {
			case ActionType.START => {
				_active += 1
				_waiting -= 1
			}
			case ActionType.END => {
				activeBuffer += 1
				doneBuffer += 1
			}
			case _ =>
		}
	}

	def active = {
		_active -= activeBuffer
		activeBuffer = 0
		_active
	}

	def waiting = _waiting

	def done = {
		_done += doneBuffer
		doneBuffer = 0
		_done
	}
}
