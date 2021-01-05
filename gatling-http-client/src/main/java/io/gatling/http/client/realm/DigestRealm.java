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

import io.gatling.http.client.uri.Uri;
import io.netty.handler.codec.http.HttpMethod;

public class DigestRealm implements Realm {

  private final String username;
  private final String password;

  public DigestRealm(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public String computeAuthorizationHeader(HttpMethod requestMethod, Uri requestUri, String authenticateHeader) {

    return new DigestAuth(
            username,
            password,
            match(authenticateHeader, "realm"),
            match(authenticateHeader, "nonce"),
            match(authenticateHeader, "opaque"),
            match(authenticateHeader, "algorithm"),
            parseRawQop(match(authenticateHeader, "qop")),
            "00000001", // FIXME
            requestMethod,
            requestUri.toRelativeUrl()
    ).computeAuthorization();
  }

  // TODO: A Pattern/Matcher may be better.
  private static String match(String headerLine, String token) {
    if (headerLine == null) {
      return null;
    }

    int match = headerLine.indexOf(token);
    if (match <= 0) {
      return null;
    }

    // = to skip
    match += token.length() + 1;
    int trailingComa = headerLine.indexOf(",", match);
    String value = headerLine.substring(match, trailingComa > 0 ? trailingComa : headerLine.length());
    value = value.length() > 0 && value.charAt(value.length() - 1) == '"'
            ? value.substring(0, value.length() - 1)
            : value;
    return value.charAt(0) == '"' ? value.substring(1) : value;
  }

  private static String parseRawQop(String rawQop) {
    if (rawQop == null) {
      return null;
    }

    String[] rawServerSupportedQops = rawQop.split(",");
    String[] serverSupportedQops = new String[rawServerSupportedQops.length];
    for (int i = 0; i < rawServerSupportedQops.length; i++) {
      serverSupportedQops[i] = rawServerSupportedQops[i].trim();
    }

    // prefer auth over auth-int
    for (String rawServerSupportedQop : serverSupportedQops) {
      if (rawServerSupportedQop.equals("auth"))
        return rawServerSupportedQop;
    }

    for (String rawServerSupportedQop : serverSupportedQops) {
      if (rawServerSupportedQop.equals("auth-int"))
        return rawServerSupportedQop;
    }

    return null;
  }
}
