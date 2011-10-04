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
package com.excilys.ebi.gatling.core.action.builder

import com.excilys.ebi.gatling.core.action.Action
import com.excilys.ebi.gatling.core.log.Logging

/**
 * This trait represents an Action Builder
 */
trait AbstractActionBuilder extends Logging {
  /**
   * Builds the Action
   *
   * @param scenarioId The Id of the current User
   * @return The built Action
   */
  def build(scenarioId: Int): Action

  /**
   * Adds next action to this builder, to be able to chain the actions
   *
   * @param next Action that will be executed after the one built by this builder
   * @return A builder of the same type, with next set
   */
  def withNext(next: Action): AbstractActionBuilder

  /**
   * Adds group information to current action
   *
   * @param groups The List of groups to which the built action will belong
   * @return A builder of the same type, with groups set
   */
  def inGroups(groups: List[String]): AbstractActionBuilder
}