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
package com.excilys.ebi.gatling.core.result.message

import java.util.concurrent.CountDownLatch
import org.joda.time.DateTime

/**
 * This case class is to be sent to the logging actor, it contains all the information
 * required for its initialization
 *
 * @param runOn the date on which the simulation started
 * @param latch the countdown latch that will end the simulation
 */
case class InitializeDataWriter(runOn: DateTime, latch: CountDownLatch)