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

import io.gatling.core.session.{ Session, SessionAttribute }

class SessionSample {

  {
    //#sessions-are-immutable
    val session: Session = ???

    // wrong usage
    session.set("foo", "FOO") // wrong: the result of this set call is just discarded
    session.set("bar", "BAR")

    // proper usage
    session.set("foo", "FOO").set("bar", "BAR")
    //#sessions-are-immutable

  }
  {
    //#session
    val session: Session = ???
    //#session

    //#session-attribute
    val attribute: SessionAttribute = session("foo")
    //#session-attribute
  }
}
