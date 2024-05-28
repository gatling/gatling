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

//#sessions-are-immutable
// wrong usage: result from Session#set is discarded
exec((session) => {
  session.set("foo", "bar");
  return session;
});

// correct usage
exec((session) => {
  const newSession = session.set("foo", "bar");
  return newSession;
});
//#sessions-are-immutable

//#set
// set one single attribute
const newSession1 = session.set("key", "whateverValue");
// set multiple attributes
const newSession2 = session.setAll({ "key": "value" });
// remove one single attribute
const newSession3 = session.remove("key");
// remove multiple attributes
const newSession4 = session.removeAll("key1", "key2");
// remove all non Gatling internal attributes
const newSession5 = session.reset();
//#set

//#get
// check if an attribute is stored in the session
const contains = session.contains("key");

// get an attribute value
const value = session.get("key");
//#get

//#properties
// the unique id of this virtual user
const userId = session.userId();
// the name of the scenario this virtual user executes
const scenario = session.scenario();
// the groups this virtual user is currently in
const groups = session.groups();
//#properties

//#state
// return true if the virtual user has experienced a failure before this point
const failed = session.isFailed();
// reset the state to success
// so that interrupt mechanisms such as exitHereIfFailed don't trigger
const newSession1 = session.markAsSucceeded();
// force the state to failure
// so that interrupt mechanisms such as exitHereIfFailed do trigger
const newSession2 = session.markAsFailed();
//#state
