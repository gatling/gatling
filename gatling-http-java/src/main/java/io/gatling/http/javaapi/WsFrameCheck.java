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

package io.gatling.http.javaapi;

import io.gatling.http.check.ws.WsCheck;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public abstract class WsFrameCheck {

  private WsFrameCheck() {
  }

  public static final class Binary extends WsFrameCheck {
    private final io.gatling.http.check.ws.WsFrameCheck.Binary wrapped;

    public Binary(io.gatling.http.check.ws.WsFrameCheck.Binary wrapped) {
      this.wrapped = wrapped;
    }

    public Binary matching(WsCheck.Binary... newMatchConditions) {
      return new Binary(wrapped.matching(toScalaSeq(newMatchConditions)));
    }

    public Binary check(WsCheck.Binary... newChecks) {
      return new Binary(wrapped.check(toScalaSeq(newChecks)));
    }

    public Binary silent() {
      return new Binary(wrapped.silent());
    }
  }

  public static final class Text extends WsFrameCheck {
    private final io.gatling.http.check.ws.WsFrameCheck.Text wrapped;

    public Text(io.gatling.http.check.ws.WsFrameCheck.Text wrapped) {
      this.wrapped = wrapped;
    }

    public Text matching(WsCheck.Text... newMatchConditions) {
      return new Text(wrapped.matching(toScalaSeq(newMatchConditions)));
    }

    public Text check(WsCheck.Text... newChecks) {
      return new Text(wrapped.check(toScalaSeq(newChecks)));
    }

    public Text silent() {
      return new Text(wrapped.silent());
    }
  }
}
