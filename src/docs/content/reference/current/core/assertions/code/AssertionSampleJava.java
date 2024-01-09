/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Simulation;

import static io.gatling.javaapi.core.CoreDsl.*;

class AssertionSampleJava extends Simulation {

  AssertionSampleJava() {
    PopulationBuilder population = scenario("foo").injectOpen(atOnceUsers(1));

//#setUp
setUp(population)
  .assertions(
    global().responseTime().max().lt(50),
    global().successfulRequests().percent().gt(95.0)
  );
//#setUp

//#details
details("MyRequest");
//#details

//#details-group
details("MyGroup", "MyRequest");
//#details-group

//#examples
// Assert that the max response time of all requests is less than 100 ms
setUp(population)
  .assertions(global().responseTime().max().lt(100));

// Assert that every request has no more than 5% of failing requests
setUp(population)
  .assertions(forAll().failedRequests().percent().lte(5.0));

// Assert that the percentage of failed requests named "MyRequest" in the group "MyGroup"
// is exactly 0 %
setUp(population)
  .assertions(details("MyGroup", "MyRequest").failedRequests().percent().is(0.0));

// Assert that the rate of requests per seconds for the group "MyGroup"
setUp(population)
  .assertions(details("MyGroup").requestsPerSec().between(100.0, 1000.0));
//#examples
  }
}
