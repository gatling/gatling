/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package io.gatling.metrics.types

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MetricsSpec extends Specification {

  "getQuantile" should {
    "work when there is no measure" in {
      val metrics = new Metrics(100)
      metrics.getQuantile(91) must beCloseTo(0.0 +/- 0.01)
    }

    "work when there is one measure" in {
      val metrics = new Metrics(100)
      metrics.update(53)
      metrics.getQuantile(91) must beCloseTo(53.0 +/- 0.01)
    }

    "work with a small number of measures" in {
      val metrics = new Metrics(100)
      for (i <- 1 to 100)
        metrics.update(i)

      metrics.getQuantile(91) must beCloseTo(91.0 +/- 0.1)
    }

    "work with a large number of measures" in {
      val metrics = new Metrics(100)
      for (i <- 1 to 10000)
        metrics.update(i)

      metrics.getQuantile(91) must beCloseTo(9100.0 +/- 0.1)
    }

    "work with a low percentiles" in {
      val metrics = new Metrics(100)
      for (i <- 1 to 10000)
        metrics.update(i)

      metrics.getQuantile(0) must beCloseTo(1.0 +/- 0.1) and (
        metrics.getQuantile(1) must beCloseTo(100.0 +/- 1))
    }

    "work with a large percentiles" in {
      val metrics = new Metrics(100)
      for (i <- 1 to 10000)
        metrics.update(i)
      metrics.getQuantile(99) must beCloseTo(9900.0 +/- 0.1) and (
        metrics.getQuantile(100) must beCloseTo(10000.0 +/- 0.1))
    }

    "work after a reset" in {
      val metrics = new Metrics(100)
      for (i <- 1 to 10000)
        metrics.update(i)
      metrics.reset
      for (i <- 1 to 100)
        metrics.update(i)
      metrics.getQuantile(100) must beCloseTo(100.0 +/- 0.1)
    }
  }
}
