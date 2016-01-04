/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
import io.gatling.core.Predef._

class PassingParameters extends Simulation {

  //#string-property
  val foo = System.getProperty("foo")
  //#string-property

  val scn = scenario("foo")

  //#injection-from-props
  val nbUsers = Integer.getInteger("users", 1)
  val myRamp  = java.lang.Long.getLong("ramp", 0L)
  setUp(scn.inject(rampUsers(nbUsers) over (myRamp seconds)))
  //#injection-from-props
}
