/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.core.check

sealed trait Comp[X] {
	def apply(x: X, y: X): Boolean
}

object LessThan {
	implicit def numericLessThan[A](implicit A: Numeric[A]) = new LessThan[A] {
		def apply(x: A, y: A) = A.lt(x, y)
	}

	implicit def comparableLessThan[A <: Comparable[A]] = new LessThan[A] {
		def apply(x: A, y: A) = x.compareTo(y) < 0
	}
}
sealed trait LessThan[X] extends Comp[X]

object LessThanOrEqual {
	implicit def numericLessThanOrEqual[A](implicit A: Numeric[A]) = new LessThanOrEqual[A] {
		def apply(x: A, y: A) = A.lteq(x, y)
	}

	implicit def comparableLessThanOrEqual[A <: Comparable[A]] = new LessThanOrEqual[A] {
		def apply(x: A, y: A) = x.compareTo(y) <= 0
	}
}
sealed trait LessThanOrEqual[X] extends Comp[X]

object GreaterThan {
	implicit def numericGreaterThan[A](implicit A: Numeric[A]) = new GreaterThan[A] {
		def apply(x: A, y: A) = A.lt(x, y)
	}

	implicit def comparableGreaterThan[A <: Comparable[A]] = new GreaterThan[A] {
		def apply(x: A, y: A) = x.compareTo(y) < 0
	}
}
sealed trait GreaterThan[X] extends Comp[X]

object GreaterThanOrEqual {
	implicit def numericGreaterThanOrEqual[A](implicit A: Numeric[A]) = new GreaterThanOrEqual[A] {
		def apply(x: A, y: A) = A.lteq(x, y)
	}

	implicit def comparableGreaterThanOrEqual[A <: Comparable[A]] = new GreaterThanOrEqual[A] {
		def apply(x: A, y: A) = x.compareTo(y) <= 0
	}
}
sealed trait GreaterThanOrEqual[X] extends Comp[X]
