/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.core.util

import scala.math.{ abs, log, sqrt }

object Erf {

	val invP1 = Vector(
		0.160304955844066229311e2,
		-0.90784959262960326650e2,
		0.18644914861620987391e3,
		-0.16900142734642382420e3,
		0.6545466284794487048e2,
		-0.864213011587247794e1,
		0.1760587821390590)

	val invQ1 = Vector(
		0.147806470715138316110e2,
		-0.91374167024260313396e2,
		0.21015790486205317714e3,
		-0.22210254121855132366e3,
		0.10760453916055123830e3,
		-0.206010730328265443e2,
		0.1e1)

	val invP2 = Vector(
		-0.152389263440726128e-1,
		0.3444556924136125216,
		-0.29344398672542478687e1,
		0.11763505705217827302e2,
		-0.22655292823101104193e2,
		0.19121334396580330163e2,
		-0.5478927619598318769e1,
		0.237516689024448000)

	val invQ2 = Vector(
		-0.108465169602059954e-1,
		0.2610628885843078511,
		-0.24068318104393757995e1,
		0.10695129973387014469e2,
		-0.23716715521596581025e2,
		0.24640158943917284883e2,
		-0.10014376349783070835e2,
		0.1e1)

	val invP3 = Vector(
		0.56451977709864482298e-4,
		0.53504147487893013765e-2,
		0.12969550099727352403,
		0.10426158549298266122e1,
		0.28302677901754489974e1,
		0.26255672879448072726e1,
		0.20789742630174917228e1,
		0.72718806231556811306,
		0.66816807711804989575e-1,
		-0.17791004575111759979e-1,
		0.22419563223346345828e-2)

	val invQ3 = Vector(
		0.56451699862760651514e-4,
		0.53505587067930653953e-2,
		0.12986615416911646934,
		0.10542932232626491195e1,
		0.30379331173522206237e1,
		0.37631168536405028901e1,
		0.38782858277042011263e1,
		0.20372431817412177929e1,
		0.1e1)

	def erfinv(n: Double) = {

		def evalPolynom(p: Seq[Double], x: Double) = p.reduceRight((c, v) => v * x + c)

		def positiveErfinv(u: Double) =
			u match {
				case _ if u >= 1.0 => Double.MaxValue
				case _ if u <= 0.75 => {
					val t = u * u - 0.5625
					val v = evalPolynom(invP1, t)
					val w = evalPolynom(invQ1, t)
					(v / w) * u
				}
				case _ if u <= 0.9375 => {
					val t = u * u - 0.87890625
					val v = evalPolynom(invP2, t)
					val w = evalPolynom(invQ2, t)
					(v / w) * u

				}
				case _ => {
					val t = 1.0 / sqrt(-log(1.0 - u))
					val v = evalPolynom(invP3, t)
					val w = evalPolynom(invQ3, t)
					(v / w) / t
				}
			}

		require(abs(n) <= 1.0, s"n=$n is not in [-1, 1]")

		if (n < 0.0) -positiveErfinv(-n) else positiveErfinv(n)
	}
}