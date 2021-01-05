/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.structure

import io.gatling.core.action.builder.FeedBuilder
import io.gatling.core.feeder._
import io.gatling.core.session._

object Feeds {

  private val OneExpression = 1.expressionSuccess
}

private[structure] trait Feeds[B] extends Execs[B] {

  /**
   * Chain an action that will inject a single data record into the virtual users' Session
   *
   * @param feederBuilder a factory of a source of records
   */
  def feed(feederBuilder: FeederBuilder): B =
    feed(feederBuilder, Feeds.OneExpression)

  /**
   * Chain an action that will inject multiple data records into the virtual users' Session
   *
   * @param feederBuilder a factory of a source of records
   * @param number the number of records to be injected
   */
  def feed(feederBuilder: FeederBuilder, number: Expression[Int]): B =
    exec(new FeedBuilder(feederBuilder, number))

  /**
   * Chain an action that will inject a single data record into the virtual users' Session
   *
   * @param feeder a source of records
   */
  def feed(feeder: Feeder[Any]): B =
    feed(feeder, Feeds.OneExpression)

  /**
   * Chain an action that will inject multiple data records into the virtual users' Session
   *
   * @param feeder a source of records
   * @param number the number of records to be injected
   */
  def feed(feeder: Feeder[Any], number: Expression[Int]): B =
    feed(() => feeder, number)
}
