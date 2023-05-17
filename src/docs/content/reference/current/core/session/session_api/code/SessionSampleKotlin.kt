/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

import io.gatling.javaapi.core.Session

import io.gatling.javaapi.core.CoreDsl.*

class SessionSampleKotlin {

  private class MyPojo
  val session: Session = TODO()

  init {
//#sessions-are-immutable
// wrong usage: result from Session#set is discarded
exec { session ->
  session.set("foo", "bar")
  println(session)
  session
}

// correct usage
exec { session ->
  val newSession = session.set("foo", "bar")
  println(newSession)
  newSession
}
//#sessions-are-immutable

//#set
// set one single attribute
val newSession1 = session.set("key", "whateverValue")
// set multiple attributes
val newSession2 = session.setAll(mapOf("key" to "value"))
// remove one single attribute
val newSession3 = session.remove("key")
// remove multiple attributes
val newSession4 = session.removeAll("key1", "key2")
// remove all non Gatling internal attributes
val newSession5 = session.reset()
//#set

//#get
// check if an attribute is stored in the session
val contains = session.contains("key")

// get an attribute value and cast it
val string = session.getString("key")

// get an Int attribute (will throw if it's null)
val primitiveInt = session.getInt("key")
// get an Int? attribute
val intWrapper = session.getIntegerWrapper("key")

// get a Long attribute (will throw if it's null)
val primitiveLong = session.getLong("key")
// get a Long? attribute
val longWrapper = session.getLongWrapper("key")

// get a Boolean attribute (will throw if it's null)
val primitiveBoolean = session.getBoolean("key")
// get a Boolean? attribute
val booleanWrapper = session.getBooleanWrapper("key")

// get a Double attribute (will throw if it's null)
val primitiveDouble = session.getDouble("key")
// get a Double? attribute
val doubleWrapper = session.getDoubleWrapper("key")

// get an attribute value and cast it into a List
val list: List<MyPojo> = session.getList("key")
// get an attribute value and cast it into a Set
val set: Set<MyPojo> = session.getSet("key")
// get an attribute value and cast it into a Map
val map: Map<String, MyPojo> = session.getMap("key")
// get an attribute value and cast it
val myPojoOrNull: MyPojo? = session.get("key")
//#get
  }

  init {
//#properties
// the unique id of this virtual user
val userId = session.userId()
// the name of the scenario this virtual user executes
val scenario = session.scenario()
// the groups this virtual user is currently in
val groups = session.groups()
//#properties
  }

  init {
//#state
// return true if the virtual user has experienced a failure before this point
val failed: Boolean = session.isFailed()
// reset the state to success
// so that interrupt mechanisms such as exitHereIfFailed don't trigger
val newSession1: Session = session.markAsSucceeded()
// force the state to failure
// so that interrupt mechanisms such as exitHereIfFailed do trigger
val newSession2: Session = session.markAsFailed()
//#state
  }
}
