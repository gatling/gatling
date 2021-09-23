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

import io.gatling.core.action.builder.ActionBuilder;
import io.gatling.core.javaapi.Session;
import io.gatling.http.Predef$;
import io.gatling.http.check.ws.WsFrameCheck;

import java.util.function.Function;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public class Ws {
  private final io.gatling.http.action.ws.Ws wrapped;

  public Ws(final io.gatling.http.action.ws.Ws wrapped) {
    this.wrapped = wrapped;
  }

  public Ws wsName(String wsName) {
    return new Ws(wrapped.wsName(toStringExpression(wsName)));
  }

  public Ws wsName(Function<Session, String> wsName) {
    return new Ws(wrapped.wsName(toTypedGatlingSessionFunction(wsName)));
  }

  public WsConnectActionBuilder connect(String url) {
    return new WsConnectActionBuilder(wrapped.connect(toStringExpression(url)));
  }

  public WsConnectActionBuilder connect(Function<Session, String> url) {
    return new WsConnectActionBuilder(wrapped.connect(toTypedGatlingSessionFunction(url)));
  }

  public WsSendTextActionBuilder sendText(String text) {
    return new WsSendTextActionBuilder(wrapped.sendText(toStringExpression(text)));
  }

  public WsSendTextActionBuilder sendText(Function<Session, String> text) {
    return new WsSendTextActionBuilder(wrapped.sendText(toTypedGatlingSessionFunction(text)));
  }

  public WsSendBinaryActionBuilder sendBytes(byte[] bytes) {
    return new WsSendBinaryActionBuilder(wrapped.sendBytes(toStaticValueExpression(bytes)));
  }

  public WsSendBinaryActionBuilder sendBytes(String bytes) {
    return new WsSendBinaryActionBuilder(wrapped.sendBytes(toBytesExpression(bytes)));
  }

  public WsSendBinaryActionBuilder sendBytes(Function<Session, byte[]> bytes) {
    return new WsSendBinaryActionBuilder(wrapped.sendBytes(toTypedGatlingSessionFunction(bytes)));
  }

  // TODO
//  public SseSetCheckActionBuilder setCheck() {
//    return new SseSetCheckActionBuilder(wrapped.setCheck());
//  }

  public ActionBuilder close() {
    return wrapped.close();
  }

  public static final class Prefix {

    public static final Prefix INSTANCE = new Prefix();

    private Prefix() {
    }

    public WsFrameCheck.Text checkTextMessage(String name) {
      return Predef$.MODULE$.ws().checkTextMessage(name);
    }

    public WsFrameCheck.Binary checkBinaryMessage(String name) {
      return Predef$.MODULE$.ws().checkBinaryMessage(name);
    }
  }
}
