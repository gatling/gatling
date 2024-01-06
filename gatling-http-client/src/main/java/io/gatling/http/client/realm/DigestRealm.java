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

package io.gatling.http.client.realm;

import io.gatling.http.client.uri.Uri;
import io.netty.handler.codec.http.HttpMethod;

public class DigestRealm implements Realm {

  @FunctionalInterface
  public interface AuthorizationGen {
    String apply(HttpMethod requestMethod, Uri requestUri, String username, String password);
  }

  public final String username;
  public final String password;
  private final AuthorizationGen authorizationHeaderF;

  public DigestRealm(String username, String password) {
    this(username, password, null);
  }

  private DigestRealm(String username, String password, AuthorizationGen authorizationHeaderF) {
    this.username = username;
    this.password = password;
    this.authorizationHeaderF = authorizationHeaderF;
  }

  public DigestRealm withAuthorizationGen(AuthorizationGen authorizationHeaderF) {
    return new DigestRealm(username, password, authorizationHeaderF);
  }

  public String getAuthorizationHeader(HttpMethod requestMethod, Uri requestUri) {
    if (authorizationHeaderF == null) {
      throw new UnsupportedOperationException("authorizationHeaderF is not configured");
    }
    return authorizationHeaderF.apply(requestMethod, requestUri, username, password);
  }

  @Override
  public String toString() {
    return "DigestRealm{username='" + username + "', password='*******'}";
  }
}
