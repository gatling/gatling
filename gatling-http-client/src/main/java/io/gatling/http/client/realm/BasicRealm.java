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

package io.gatling.http.client.realm;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BasicRealm implements Realm {

  private final String username;
  private final String password;
  private final String authorizationHeader;

  public BasicRealm(String username, String password) {
    this.username = username;
    this.password = password;
    this.authorizationHeader = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(UTF_8));
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getAuthorizationHeader() {
    return authorizationHeader;
  }

  @Override
  public String toString() {
    return "BasicRealm{username='" + username + "', password='*******'}";
  }
}
