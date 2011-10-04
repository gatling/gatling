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

import com.excilys.ebi.gatling.core.action.request.AbstractRequest
import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.processor.builder.ProcessorBuilder

/**
 * Abstract class for all request actions. For example HTTPRequestAction, and later LDAPRequestAction, etc.
 *
 * @param next action that will be executed after the request
 * @param request request that will be sent
 * @param givenProcessors a list of processors that will apply on the response
 * @param groups a list of groups in which this action is
 */
abstract class RequestAction(next: Action, request: AbstractRequest, givenProcessors: Option[List[ProcessorBuilder]], groups: List[String]) extends Action {
  def execute(context: Context)
}