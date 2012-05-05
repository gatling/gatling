
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

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import scala.math.abs

@RunWith(classOf[JUnitRunner])
class NumberHelperSpec extends Specification {

  "getRandomLong" should {
    "produce uniformly-distributed random numbers within the specified range" in {
      val min:Long = 1; val max:Long = 100;
      val numSamples: Int = 10000

      var sum:Long = 0

      for(i <- 0 until numSamples) {
        val uniformSample: Long = NumberHelper.getRandomLong(min, max)

        uniformSample must beGreaterThanOrEqualTo(min)
        uniformSample must beLessThanOrEqualTo(max)

        sum += uniformSample
       }

      val average:Long = sum / numSamples

      average must beCloseTo( max / 2, 1)
    }
  }

  "getRandomDoubleFromExponential" should {
    "produce exponentially-distributed random numbers around the specified average" in {
      val expectedAverage: Long = 10
      val numSamples: Int = 1000
      var timesCloseToExpectedAverage = 0
      var numTests = 100

      for (n <- 0 until numTests) {
        var samples = List[Double]()
        var sum: Double = 0

        for (i <- 0 until numSamples) {
          val exponentialSample: Double = NumberHelper.getRandomDoubleFromExp(expectedAverage)
          samples ::= exponentialSample
          sum += exponentialSample
        }

        samples.length mustEqual (numSamples)

        val average: Double = sum / numSamples
        if (abs(average - expectedAverage) < 1.0){
          timesCloseToExpectedAverage += 1
        }
      }

      val percentageOfTestsWithinRange: Double = timesCloseToExpectedAverage.toDouble / numTests.toDouble
      percentageOfTestsWithinRange must beGreaterThanOrEqualTo(0.99)
    }
  }

}