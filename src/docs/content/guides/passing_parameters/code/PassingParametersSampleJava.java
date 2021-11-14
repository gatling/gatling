/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import static io.gatling.javaapi.core.CoreDsl.*;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;

class PassingParametersSampleJava extends Simulation {

  {
ScenarioBuilder scn = scenario("foo");

//#injection-from-props
int nbUsers = Integer.getInteger("users", 1);
long myRamp = Long.getLong("ramp", 0);

setUp(scn.injectOpen(rampUsers(nbUsers).during(myRamp)));
//#injection-from-props
  }
}
