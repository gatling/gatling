package com.excilys.ebi.gatling.core.scenario

import com.excilys.ebi.gatling.core.action.Action
import java.util.concurrent.TimeUnit

class Scenario(name: String, firstAction: Action) {
	def getFirstAction = firstAction
}