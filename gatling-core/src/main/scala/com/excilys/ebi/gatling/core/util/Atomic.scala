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

import java.util.concurrent.atomic.AtomicReference

import annotation.tailrec

object Atomic {
	def apply[T](obj: T) = new Atomic(new AtomicReference(obj))
	implicit def toAtomic[T](ref: AtomicReference[T]): Atomic[T] = new Atomic(ref)
}

class Atomic[T](val atomic: AtomicReference[T]) {
	@tailrec
	final def update(f: T => T): T = {
		val oldValue = atomic.get()
		val newValue = f(oldValue)
		if (atomic.compareAndSet(oldValue, newValue)) newValue else update(f)
	}

	def get = atomic.get
}