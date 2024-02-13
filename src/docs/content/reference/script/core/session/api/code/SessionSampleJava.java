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

import io.gatling.javaapi.core.Session;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.gatling.javaapi.core.CoreDsl.*;

class SessionSampleJava {

  Session session = null;
  {
//#sessions-are-immutable
// wrong usage: result from Session#set is discarded
exec(session -> {
  session.set("foo", "bar");
  return session;
});

// correct usage
exec(session -> {
  Session newSession = session.set("foo", "bar");
  return newSession;
});
//#sessions-are-immutable
  }

  {
//#set
// set one single attribute
Session newSession1 = session.set("key", "whateverValue");
// set multiple attributes
Session newSession2 = session.setAll(Map.of("key", "value"));
// remove one single attribute
Session newSession3 = session.remove("key");
// remove multiple attributes
Session newSession4 = session.removeAll("key1", "key2");
// remove all non Gatling internal attributes
Session newSession5 = session.reset();
//#set
  }

  private static class MyPojo {}

  {
//#get
// check if an attribute is stored in the session
boolean contains = session.contains("key");

// get an attribute value and cast it
String string = session.getString("key");

// get an int attribute (will throw if it's null)
int primitiveInt = session.getInt("key");
// get an Integer attribute
Integer intWrapper = session.getIntegerWrapper("key");

// get a long attribute (will throw if it's null)
long primitiveLong = session.getLong("key");
// get a Long attribute
Long longWrapper = session.getLongWrapper("key");

// get a boolean attribute (will throw if it's null)
boolean primitiveBoolean = session.getBoolean("key");
// get a Boolean attribute
Boolean booleanWrapper = session.getBooleanWrapper("key");

// get a double attribute (will throw if it's null)
double primitiveDouble = session.getDouble("key");
// get a Double attribute
Double doubleWrapper = session.getDoubleWrapper("key");

// get an attribute value and cast it into a List
List<MyPojo> list = session.getList("key");
// get an attribute value and cast it into a Set
Set<MyPojo> set = session.getSet("key");
// get an attribute value and cast it into a Map
Map<String, MyPojo> map = session.getMap("key");
// get an attribute value and cast it
MyPojo myPojo = session.get("key");
//#get
  }

  {
//#properties
// the unique id of this virtual user
long userId = session.userId();
// the name of the scenario this virtual user executes
String scenario = session.scenario();
// the groups this virtual user is currently in
List<String> groups = session.groups();
//#properties
  }

  {
//#state
// return true if the virtual user has experienced a failure before this point
boolean failed = session.isFailed();
// reset the state to success
// so that interrupt mechanisms such as exitHereIfFailed don't trigger
Session newSession1 = session.markAsSucceeded();
// force the state to failure
// so that interrupt mechanisms such as exitHereIfFailed do trigger
Session newSession2 = session.markAsFailed();
//#state
  }
}
