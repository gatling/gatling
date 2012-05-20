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
package com.excilys.ebi.gatling.core.util

import java.util.Random
import scala.math.round
import org.apache.commons.math3.distribution.ExponentialDistribution
;

object NumberHelper {

  /**
   * Used to generate random pause durations
   */
  val RANDOM = new Random

  /**
   * Get a random long from a uniform distribution between the values min and max.
   * @param min is the minimum value of the uniform distribution
   * @param max is the maximum value of the uniform distribution
   * @return
   */
  def getRandomLong(min: Long, max: Long): Long = round(min.toDouble + (RANDOM.nextDouble * (max - min).toDouble))

  /**
   * Get a random double from an exponential distribution with the specified average value.
   *
   * @param avg is the desired average value of the exponential distribution
   * @return
   * @see http://perfdynamics.blogspot.com/2012/03/how-to-generate-exponential-delays.html#more
   */
  def getRandomDoubleFromExp(avg: Double): Double = new ExponentialDistribution(avg).sample()

  /**
   * Get a random long from an exponential distribution with the specified average value.
   *
   * @param avg is the desired average value of the exponential distribution
   * @return
   * @see http://perfdynamics.blogspot.com/2012/03/how-to-generate-exponential-delays.html#more
   */
  def getRandomLongFromExp(avg: Double): Long = round(getRandomDoubleFromExp(avg))

  def isNumeric(string: String) = string.forall(_.isDigit)
}