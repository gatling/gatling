/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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
import io.gatling.core.session.el._

private[structure] trait Feeds[B] extends Execs[B] {

  /**
   * Chain an action that will inject a single data record into the virtual users' Session
   *
   * @param feederBuilder a factory of a source of records
   */
  def feed(feederBuilder: FeederBuilder): B =
    feed0(feederBuilder, System.identityHashCode(feederBuilder), None)

  /**
   * Chain an action that will inject multiple data records into the virtual users' Session
   *
   * @param feederBuilder a factory of a source of records
   * @param number the number of records to be injected
   */
  def feed(feederBuilder: FeederBuilder, number: Expression[Int]): B =
    feed0(feederBuilder, System.identityHashCode(feederBuilder), Some(number))

  private[gatling] def feed0(feederBuilder: FeederBuilder, feederBuilderKey: Long, number: Option[Expression[Int]]): B =
    exec(new FeedBuilder(feederBuilder, feederBuilderKey, number))

  /**
   * Chain an action that will inject a single data record into the virtual users' Session
   *
   * @param feeder a source of records
   */
  def feed(feeder: Feeder[Any]): B =
    feed0(feeder, System.identityHashCode(feeder), None)

  /**
   * Chain an action that will inject multiple data records into the virtual users' Session
   *
   * @param feeder a source of records
   * @param number the number of records to be injected
   */
  def feed(feeder: Feeder[Any], number: String): B =
    feed0(feeder, System.identityHashCode(feeder), Some(number.el[Int]))

  /**
   * Chain an action that will inject multiple data records into the virtual users' Session
   *
   * @param feeder a source of records
   * @param number the number of records to be injected
   */
  def feed(feeder: Feeder[Any], number: Expression[Int]): B =
    feed0(feeder, System.identityHashCode(feeder), Some(number))

  private[gatling] def feed0(feeder: Feeder[Any], feederBuilderKey: Long, number: Option[Expression[Int]]): B =
    feed0(() => feeder, feederBuilderKey, number)
}
