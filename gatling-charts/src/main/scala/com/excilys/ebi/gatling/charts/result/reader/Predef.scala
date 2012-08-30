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
package com.excilys.ebi.gatling.charts.result.reader

import com.excilys.ebi.gatling.charts.result.reader.stats.StatPipe

import cascading.pipe.Pipe

object Predef {
	implicit def pipeToStatPipe(pipe: Pipe): StatPipe = new StatPipe(pipe)

	implicit def symbolToString(symbol: Symbol): String = symbol.name

	val LOG_STEP = 100000
	val SEC_MILLISEC_RATIO = 1000.0
}
