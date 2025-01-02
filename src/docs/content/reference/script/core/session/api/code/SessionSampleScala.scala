/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.validation.Validation
import io.gatling.core.Predef._
import io.gatling.core.session.Session


class SessionSampleScala {
val session: Session = ???

  {
//#sessions-are-immutable
// wrong usage: result from Session#set is discarded
exec { session =>
  session.set("foo", "bar")
  session
}

// correct usage
exec { session =>
  val newSession = session.set("foo", "bar")
  newSession
}
//#sessions-are-immutable
  }

  {
//#set
// set one single attribute
val newSession1 = session.set("key", "whateverValue")
// set multiple attributes
val newSession2 = session.setAll(Map("key" -> "value"))
// remove one single attribute
val newSession3 = session.remove("key")
// remove multiple attributes
val newSession4 = session.removeAll("key1", "key2")
// remove all non Gatling internal attributes
val newSession5 = session.reset
//#set
  }

  private case class MyPojo()

  {
//#get
// check if an attribute is stored in the session
val contains = session.contains("key")

// get an attribute value and convert it to the type parameter
// throws if key is undefined or actual type doesn't match
val string = session("key").as[String]
// get an attribute value and convert it to an Option of the type parameter
// throws if actual type doesn't match
val stringOpt: Option[String] = session("key").asOption[String]
// get an attribute value and convert it to a Validation of the type parameter
// never throws
val stringV: Validation[String] = session("key").validate[String]
//#get
  }

  {
//#properties
// the unique id of this virtual user
val userId = session.userId
// the name of the scenario this virtual user executes
val scenario = session.scenario
// the groups this virtual user is currently in
val groups = session.groups
//#properties
  }

  {
//#state
// return true if the virtual user has experienced a failure before this point
val failed = session.isFailed
// reset the state to success
// so that interrupt mechanisms such as exitHereIfFailed don't trigger
val newSession1 = session.markAsSucceeded
// force the state to failure
// so that interrupt mechanisms such as exitHereIfFailed do trigger
val newSession2 = session.markAsFailed
//#state
  }
}
