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
package com.excilys.ebi.gatling.log.stats

import com.excilys.ebi.gatling.log.Predef._
import cascading.pipe.Pipe
import cascading.tuple.Fields
import com.twitter.scalding.GroupBuilder
import com.twitter.scalding.Dsl._
import com.excilys.ebi.gatling.log.stats.StatsHelper._
import com.excilys.ebi.gatling.log.util.FieldsNames._

class StatPipe(val pipe: Pipe) {

	def distributionSize[A, B](bucketField: Fields, groupFields: Fields) =
		pipe.distribution(bucketField, groupFields) {
			_.size
		}

	def distributionMax[A, B](bucketField: Fields, groupFields: Fields, maxField: Fields) =
		pipe.distribution(bucketField, groupFields) {
			_.max((maxField, maxField))
		}

	def distribution[A, B](bucketField: Fields, groupFields: Fields)(groupFunction: GroupBuilder => GroupBuilder) = {
		assert(bucketField.size() == 1, "Must specify exactly one Field name for the bucketField")
		pipe
			.groupBy(groupFields.append(bucketField))(groupFunction)
	}

	def distributionWithBuckets[A, B](inputField: Fields, outputField: Fields, bucketSizeField: Fields, distributionField: Fields, groupFields: Fields)(bucket: A => B) = {
		assert(inputField.size() == 1, "Must specify exactly one Field name for the inputField")
		assert(outputField.size() == 1, "Must specify exactly one Field name for the outputField")
		assert(distributionField.size() == 1, "Must specify exactly one Field name for the distributionField")
		assert(bucketSizeField.size() == 1, "Must specify exactly one Field name for the bucketSizeField")

		pipe.groupBy(inputField.append(groupFields)) {
			_.size('sizeUnit)
		}
			.map(inputField, outputField)(bucket)
			.groupBy(groupFields.append(outputField)) {
			_.sum('sizeUnit -> SIZE)
				.sortBy(inputField)
				.mapReduceMap((inputField.append('sizeUnit), distributionField)) {
				current: (Long, Long) => Map(current)
			} {
				(accum: Map[Long, Long], current: Map[Long, Long]) => accum ++ current
			} {
				accum: Map[Long, Long] => accum
			}
		}
			.map(bucketSizeField -> bucketSizeField) {
			d: Double => math.round(d)
		}
	}

	def groupByOrAll(fields: Fields)(fn: GroupBuilder => GroupBuilder) =
		if (fields.size == 0)
			pipe.groupAll(fn)
		else
			pipe.groupBy(fields)(fn)

	def groupByAndSum(groupFields: Fields, sumField: Fields) = {
		assert(sumField.size() == 1, "Must specify exactly one Field name for the sumField")

		pipe.groupBy(groupFields) {
			_.sum(sumField, sumField)
		}
			.map(sumField, sumField) {
			d: Double => math.round(d)
		}
	}

	def groupByAndSumAndReduce(groupFields: Fields, sumField: Fields, reduceField: Fields) = {
		assert(sumField.size() == 1, "Must specify exactly one Field name for the sumField")
		assert(reduceField.size() == 1, "Must specify exactly one Field name for the reduceField")

		pipe.groupBy(groupFields) {
			_.sum(sumField, sumField)
				.reduce((reduceField, reduceField)) {
				(map: Map[Long, Long], value: Map[Long, Long]) => mergeMap(value, map)
			}
		}
			.map(sumField, sumField) {
			d: Double => math.round(d)
		}
	}

	def joinAndSort(joinFields: (Fields, Fields), otherPipe: Pipe, sortFields: Fields, groupFields: Fields = new Fields()) =
		pipe.joinWithSmaller(joinFields, otherPipe)
			.discard(joinFields._1)
			.groupBy(groupFields.append(sortFields)) {
			g: GroupBuilder => g
		}

	def mergeStats(range: Double, groupFields: Fields = new Fields(), sizeField: Fields = SIZE, meanFields: Fields = (MEAN, SQUARE_MEAN, MEAN_LATENCY), minField: Fields = MIN, maxField: Fields = MAX, stdDevField: Fields = STD_DEV, meanRequestPerSec: Fields = MEAN_REQUEST_PER_SEC) = {
		assert(sizeField.size() == 1, "Must specify exactly one Field name for the sizeField")
		assert(meanFields.size() == 3, "Must specify exactly three Field names for the meanFields")
		assert(minField.size() == 1, "Must specify exactly one Field name for the minField")
		assert(maxField.size() == 1, "Must specify exactly one Field name for the maxField")
		assert(stdDevField.size() == 1, "Must specify exactly one Field name for the stdDevField")
		assert(meanRequestPerSec.size() == 1, "Must specify exactly one Field name for the meanRequestPerSec")
		pipe.groupByOrAll(groupFields) {
			_.reduce((sizeField.append(meanFields), sizeField.append(meanFields)))(mergeMeans)
				.min((minField, minField))
				.max((maxField, maxField))
		}
			.map((sizeField.append(meanFields), stdDevField.append(meanRequestPerSec))) {
			t: (Long, Double, Double, Double) => (math.sqrt(t._3 - square(t._2)), t._1 / range)
		}
	}

	private def mergeMeans(partial: (Long, Double, Double, Double), actual: (Long, Double, Double, Double)) = {
		val ((countPartial, meanPartial, squareMeanPartial, meanLatencyPartial), (countActual, meanActual, squareMeanActual, meanLatencyActual)) = (partial, actual)
		val newCount = countPartial + countActual
		(newCount, (meanPartial * countPartial + meanActual * countActual) / newCount, (squareMeanPartial * countPartial + squareMeanActual * countActual) / newCount, (meanLatencyPartial * countPartial + meanLatencyActual * countActual) / newCount)
	}

	private def mergeMap(small: Map[Long, Long], big: Map[Long, Long]) =
		big ++ small.map {
			case (key, value) => key -> (value + big.getOrElse(key, 0L))
		}
}
