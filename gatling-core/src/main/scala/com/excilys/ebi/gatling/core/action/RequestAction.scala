/*
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.action

import com.excilys.ebi.gatling.core.action.request.Request
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.feeder.Feeder
import com.excilys.ebi.gatling.core.check.CheckBuilder

/**
 * Abstract class for all request actions. For example HTTPRequestAction, and later LDAPRequestAction, etc.
 *
 * @param next action that will be executed after the request
 * @param request request that will be sent
 * @param givenProcessors a list of processors that will apply on the response
 * @param groups a list of groups in which this action is
 */
abstract class RequestAction[P](next: Action, request: Request, givenProcessors: Option[List[CheckBuilder[P]]], groups: List[String], feeder: Option[Feeder]) extends Action {
	def execute(context: Context)
}