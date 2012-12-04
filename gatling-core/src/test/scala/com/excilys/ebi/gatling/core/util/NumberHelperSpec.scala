/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.core.util

import scala.math.abs

import org.apache.commons.math3.distribution.{ UniformIntegerDistribution, ExponentialDistribution }
import org.apache.commons.math3.exception.NotStrictlyPositiveException
import org.apache.commons.math3.random.EmpiricalDistribution
import org.apache.commons.math3.stat.descriptive.StatisticalSummary
import org.apache.commons.math3.stat.StatUtils
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NumberHelperSpec extends Specification {

	"getRandomLong" should {

		"be zero when min and max are zero" in {
			NumberHelper.createUniformRandomLongGenerator(0, 0)() must beEqualTo(0)
		}

		"produce uniformly-distributed random numbers within the specified range" in {
			val min: Int = 0
			val max: Int = 100
			val expectedAverage: Double = (max + min) / 2
			val numSamples: Int = 1000
			val numTests = 100
			var timesMatchingDistribution = 0

			val uniformDistribution: UniformIntegerDistribution = new UniformIntegerDistribution(min, max)

			for (n <- 0 until numTests) {
				var samples = List[Double]()

				val uniformRandomLongGenerator = NumberHelper.createUniformRandomLongGenerator(min, max)

				for (i <- 0 until numSamples) {
					val uniformSample: Long = uniformRandomLongGenerator()
					samples ::= uniformRandomLongGenerator()

					uniformSample must beGreaterThanOrEqualTo(min.toLong)
					uniformSample must beLessThanOrEqualTo(max.toLong)
				}

				val samplesArray: Array[Double] = samples.toArray
				val empiricalDistribution = new EmpiricalDistribution
				empiricalDistribution.load(samplesArray)
				val sampleStats: StatisticalSummary = empiricalDistribution.getSampleStats
				val twentyFifthPercentileVal: Long = StatUtils.percentile(samplesArray, 25.0).round
				val seventyFifthPercentileVal: Long = StatUtils.percentile(samplesArray, 75.0).round

				if (sampleStats.getN == numSamples
					&& isCloseTo(sampleStats.getMean, expectedAverage, 2)
					&& isCloseTo(uniformDistribution.cumulativeProbability(twentyFifthPercentileVal.toInt), 0.25, 0.15)
					&& isCloseTo(uniformDistribution.cumulativeProbability(seventyFifthPercentileVal.toInt), 0.75, 0.15)) {
					timesMatchingDistribution += 1
				}

			}

			val percentTestsMatchingDistribution: Double = timesMatchingDistribution.toDouble / numTests.toDouble
			percentTestsMatchingDistribution must beGreaterThanOrEqualTo(0.90)
		}
	}

	"getRandomDoubleFromExponential" should {

		"throw exception when expected average is zero" in {
			NumberHelper.createExpRandomDoubleGenerator(0) should throwA[NotStrictlyPositiveException]
		}

		"produce exponentially-distributed random doubles around the specified average" in {
			val expectedAverage: Long = 10
			val numSamples: Int = 1000
			val numTests = 100
			var timesMatchingDistribution = 0

			val exponentialDistribution: ExponentialDistribution = new ExponentialDistribution(expectedAverage)

			for (n <- 0 until numTests) {
				var samples = List[Double]()

				val expRandomDoubleGenerator = NumberHelper.createExpRandomDoubleGenerator(expectedAverage)
				for (i <- 0 until numSamples) {
					samples ::= expRandomDoubleGenerator()
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
					timesMatchingDistribution += 1
				}
			}

			val percentTestsMatchingDistribution: Double = timesMatchingDistribution.toDouble / numTests.toDouble
			percentTestsMatchingDistribution must beGreaterThanOrEqualTo(0.90)
		}
	}

	"getRandomLongFromExponential" should {

		"throw exception when expected average is zero" in {
			NumberHelper.createExpRandomLongGenerator(0) should throwA[NotStrictlyPositiveException]
		}

		"produce exponentially-distributed random longs around the specified average" in {
			val expectedAverage: Long = 10
			val numSamples: Int = 1000
			val numTests = 100
			var timesMatchingDistribution = 0

			val exponentialDistribution: ExponentialDistribution = new ExponentialDistribution(expectedAverage)

			for (n <- 0 until numTests) {
				var samples = List[Double]()

				val expRandomLongGenerator = NumberHelper.createExpRandomLongGenerator(expectedAverage)
				for (i <- 0 until numSamples) {
					samples ::= expRandomLongGenerator()
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
					timesMatchingDistribution += 1
				}
			}

			val percentTestsMatchingDistribution: Double = timesMatchingDistribution.toDouble / numTests.toDouble
			percentTestsMatchingDistribution must beGreaterThanOrEqualTo(0.90)
		}
	}

	def isCloseTo(expected: Double, actual: Double, tolerance: Double): Boolean = abs(actual - expected) <= tolerance
	def isCloseTo(expected: Long, actual: Long, tolerance: Long): Boolean = abs(actual - expected) <= tolerance

	"formatNumberWithSuffix" should {

		"return '1st' for 1" in {
			NumberHelper.formatNumberWithSuffix(1) must beEqualTo("1st")
		}

		"return '2nd' for 2" in {
			NumberHelper.formatNumberWithSuffix(2) must beEqualTo("2nd")
		}

		"return '3rd' for 3" in {
			NumberHelper.formatNumberWithSuffix(3) must beEqualTo("3rd")
		}

		"return '4th' for 4" in {
			NumberHelper.formatNumberWithSuffix(4) must beEqualTo("4th")
		}

		"return '11th' for 11" in {
			NumberHelper.formatNumberWithSuffix(11) must beEqualTo("11th")
		}

		"return '12th' for 12" in {
			NumberHelper.formatNumberWithSuffix(12) must beEqualTo("12th")
		}

		"return '13th' for 13" in {
			NumberHelper.formatNumberWithSuffix(13) must beEqualTo("13th")
		}

		"return '21st' for 21" in {
			NumberHelper.formatNumberWithSuffix(21) must beEqualTo("21st")
		}

		"return '12341st' for 12341" in {
			NumberHelper.formatNumberWithSuffix(12341) must beEqualTo("12341st")
		}

		"return '12311th' for 12311" in {
			NumberHelper.formatNumberWithSuffix(12311) must beEqualTo("12311th")
		}
	}
}