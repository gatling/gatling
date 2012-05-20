
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
import org.apache.commons.math3.distribution.ExponentialDistribution
import org.apache.commons.math3.random.EmpiricalDistribution
import org.apache.commons.math3.stat.descriptive.StatisticalSummary
import org.apache.commons.math3.stat.StatUtils

@RunWith(classOf[JUnitRunner])
class NumberHelperSpec extends Specification {

  "getRandomLong" should {

    "be zero when min and max are zero" in {
      NumberHelper.getRandomLong(0, 0) must beEqualTo(0)
    }

    "produce uniformly-distributed random numbers within the specified range" in {
      val min:Long = 0; val max:Long = 100;
      val expectedAverage:Double = (max + min) / 2
      val numSamples: Int = 1000
      var withinOneStdDev = 0
      var withinTwoStdDev = 0
      var numTests = 100

      var differences = List[Double]()

      for (n <- 0 until numTests) {
        var samples = List[Long]()
        var sum: Long = 0

        for (i <- 0 until numSamples) {
          val uniformSample: Long = NumberHelper.getRandomLong(min, max)
          samples ::= uniformSample

          uniformSample must beGreaterThanOrEqualTo(min)
          uniformSample must beLessThanOrEqualTo(max)

          sum += uniformSample
        }

        samples.length mustEqual (numSamples)

        val average: Double = sum.toDouble / numSamples
        val diffFromAverage: Double = average - expectedAverage
        differences ::= diffFromAverage
        if (abs(diffFromAverage) < 1 /* within one stddev */){
          withinOneStdDev += 1
        }

        if (abs(diffFromAverage) < 2 /* within two stddev */){
          withinTwoStdDev += 1
        }

      }

      /* expect about 68.2% of tests to have fallen within the first standard deviation */
      withinOneStdDev.toDouble / numTests.toDouble must beGreaterThanOrEqualTo(0.65)

      /* expect about 95.4% of tests to have fallen within the second standard deviation */
      withinTwoStdDev.toDouble / numTests.toDouble must beGreaterThanOrEqualTo(0.93)
    }
  }

  "getRandomDoubleFromExponential" should {

    "be zero when expectedAverage is zero" in {
      NumberHelper.getRandomDoubleFromExp(0) should beEqualTo(0)
    }

    "produce exponentially-distributed random doubles around the specified average" in {
      val expectedAverage: Long = 10
      val numSamples: Int = 1000
      val numTests = 100
      var timesCloseToExpectedAverage = 0

      val exponentialDistribution: ExponentialDistribution = new ExponentialDistribution(expectedAverage)

      for (n <- 0 until numTests) {
        var samples = List[Double]()

        for (i <- 0 until numSamples) {
          val exponentialSample: Double = NumberHelper.getRandomDoubleFromExp(expectedAverage)
          samples ::= exponentialSample
        }

        val samplesArray: Array[Double] = samples.toArray
        val empiricalDistribution = new EmpiricalDistribution
        empiricalDistribution.load(samplesArray)
        val sampleStats: StatisticalSummary = empiricalDistribution.getSampleStats
        val twentyFifthPercentileVal: Double = StatUtils.percentile(samplesArray, 25.0)
        val seventyFifthPercentileVal: Double = StatUtils.percentile(samplesArray, 75.0)

        if (sampleStats.getN == numSamples
          && isCloseTo(sampleStats.getMean, expectedAverage, 1)
          && isCloseTo(exponentialDistribution.cumulativeProbability(twentyFifthPercentileVal), 0.25, 0.05)
          && isCloseTo(exponentialDistribution.cumulativeProbability(seventyFifthPercentileVal), 0.75, 0.05)) {
          timesCloseToExpectedAverage += 1
        }
      }

      val percentTestsMatchingExponentialDist: Double = timesCloseToExpectedAverage.toDouble / numTests.toDouble
      percentTestsMatchingExponentialDist must beGreaterThanOrEqualTo(0.95)
    }
  }

  "getRandomLongFromExponential" should {

    "be zero when expectedAverage is zero" in {
      NumberHelper.getRandomLongFromExp(0) should beEqualTo(0)
    }

    "produce exponentially-distributed random longs around the specified average" in {
      val expectedAverage: Long = 10
      val numSamples: Int = 1000
      val numTests = 100
      var timesCloseToExpectedAverage = 0

      val exponentialDistribution: ExponentialDistribution = new ExponentialDistribution(expectedAverage)

      for (n <- 0 until numTests) {
        var samples = List[Double]()

        for (i <- 0 until numSamples) {
          val exponentialSample: Long = NumberHelper.getRandomLongFromExp(expectedAverage)
          samples ::= exponentialSample
        }

        val samplesArray: Array[Double] = samples.toArray
        val empiricalDistribution = new EmpiricalDistribution
        empiricalDistribution.load(samplesArray)
        val sampleStats: StatisticalSummary = empiricalDistribution.getSampleStats
        val twentyFifthPercentileVal: Double = StatUtils.percentile(samplesArray, 25.0)
        val seventyFifthPercentileVal: Double = StatUtils.percentile(samplesArray, 75.0)

        if (sampleStats.getN == numSamples
          && isCloseTo(sampleStats.getMean, expectedAverage, 1)
          && isCloseTo(exponentialDistribution.cumulativeProbability(twentyFifthPercentileVal), 0.25, 0.05)
          && isCloseTo(exponentialDistribution.cumulativeProbability(seventyFifthPercentileVal), 0.75, 0.05)) {
          timesCloseToExpectedAverage += 1
        }
      }

      val percentTestsMatchingExponentialDist: Double = timesCloseToExpectedAverage.toDouble / numTests.toDouble
      percentTestsMatchingExponentialDist must beGreaterThanOrEqualTo(0.95)
    }
  }

  def isCloseTo(expected:Double, actual:Double, tolerance:Double):Boolean = abs(actual - expected) <= tolerance
  def isCloseTo(expected:Long, actual:Long, tolerance:Long):Boolean = abs(actual - expected) <= tolerance

}