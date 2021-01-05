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

import io.gatling.netty.util.StringBuilderPool;
import io.netty.handler.codec.http.HttpMethod;

import java.security.MessageDigest;
import java.util.concurrent.ThreadLocalRandom;

import static io.gatling.http.client.util.MessageDigestUtils.pooledMd5MessageDigest;
import static io.gatling.http.client.util.StringUtils.*;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static io.gatling.http.client.util.MiscUtils.*;

class DigestAuth {

  // MD5("")
  private static final String EMPTY_ENTITY_MD5 = "d41d8cd98f00b204e9800998ecf8427e";

  private final String username;
  private final String password;
  private final String realm;
  private final String nonce;
  private final String opaque;
  private final String algorithm;
  private final String qop;
  private final String nc;
  private final HttpMethod requestMethod;
  private final String realmUri;


  DigestAuth(String username,
             String password,
             String realm,
             String nonce,
             String opaque,
             String algorithm,
             String qop,
             String nc,
             HttpMethod requestMethod,
             String realmUri) {
    this.username = username;
    this.password = password;
    this.realm = realm;
    this.nonce = nonce;
    this.opaque = opaque;
    this.algorithm = algorithm;
    this.qop = qop;
    this.nc = nc;
    this.requestMethod = requestMethod;
    this.realmUri = realmUri;
  }

  public String computeAuthorization() {
    MessageDigest md = pooledMd5MessageDigest();
    String cnonce = newCnonce(md);
    String response = newResponse(md, cnonce);

    StringBuilder builder = new StringBuilder().append("Digest ");
    append(builder, "username", username, true);
    append(builder, "realm", realm, true);
    append(builder, "nonce", nonce, true);
    append(builder, "uri", realmUri, true);
    if (isNonEmpty(algorithm)) {
      append(builder, "algorithm", algorithm, false);
    }
    append(builder, "response", response, true);

    if (opaque != null) {
      append(builder, "opaque", opaque, true);
    }

    if (qop != null) {
      append(builder, "qop", qop, false);
      // nc and cnonce only sent if server sent qop
      append(builder, "nc", nc, false);
      append(builder, "cnonce", cnonce, true);
    }
    builder.setLength(builder.length() - 2); // remove tailing ", "

    // FIXME isn't there a more efficient way?
    return new String(charSequence2Bytes(builder, ISO_8859_1));
  }

  private static String newCnonce(MessageDigest md) {
    byte[] b = new byte[8];
    ThreadLocalRandom.current().nextBytes(b);
    b = md.digest(b);
    return toHexString(b);
  }

  private String newResponse(MessageDigest md, String cnonce) {

    StringBuilder sb = StringBuilderPool.DEFAULT.get();

    // WARNING: DON'T MOVE, BUFFER IS RECYCLED!!!!
    byte[] ha1 = ha1(sb, md, cnonce);
    byte[] ha2 = ha2(sb, realmUri, md);

    appendBase16(sb, ha1);
    appendMiddlePart(sb, cnonce);
    appendBase16(sb, ha2);

    byte[] responseDigest = md5FromRecycledStringBuilder(sb, md);
    return toHexString(responseDigest);
  }

  private byte[] md5FromRecycledStringBuilder(StringBuilder sb, MessageDigest md) {
    md.update(charSequence2ByteBuffer(sb, ISO_8859_1));
    sb.setLength(0);
    return md.digest();
  }

  private byte[] ha1(StringBuilder sb, MessageDigest md, String cnonce) {
    // if algorithm is "MD5" or is unspecified => A1 = username ":" realm-value ":" passwd
    // if algorithm is "MD5-sess" => A1 = MD5( username-value ":" realm-value ":" passwd ) ":" nonce-value ":" cnonce-value

    sb.append(username).append(':').append(realm).append(':').append(password);
    byte[] core = md5FromRecycledStringBuilder(sb, md);

    if (algorithm == null || algorithm.equals("MD5")) {
      // A1 = username ":" realm-value ":" passwd
      return core;
    } else if ("MD5-sess".equals(algorithm)) {
      // A1 = MD5(username ":" realm-value ":" passwd ) ":" nonce ":" cnonce
      appendBase16(sb, core);
      sb.append(':').append(nonce).append(':').append(cnonce);
      return md5FromRecycledStringBuilder(sb, md);
    }

    throw new UnsupportedOperationException("Digest algorithm not supported: " + algorithm);
  }

  private byte[] ha2(StringBuilder sb, String digestUri, MessageDigest md) {

    // if qop is "auth" or is unspecified => A2 = Method ":" digest-uri-value
    // if qop is "auth-int" => A2 = Method ":" digest-uri-value ":" H(entity-body)
    sb.append(requestMethod.name()).append(':').append(digestUri);
    if ("auth-int".equals(qop)) {
      // when qop == "auth-int", A2 = Method ":" digest-uri-value ":" H(entity-body)
      // but we don't have the request body here
      // we would need a new API
      sb.append(':').append(EMPTY_ENTITY_MD5);

    } else if (qop != null && !qop.equals("auth")) {
      throw new UnsupportedOperationException("Digest qop not supported: " + qop);
    }

    return md5FromRecycledStringBuilder(sb, md);
  }

  private void appendMiddlePart(StringBuilder sb, String cnonce) {
    // request-digest = MD5(H(A1) ":" nonce ":" nc ":" cnonce ":" qop ":" H(A2))
    sb.append(':').append(nonce).append(':');
    if ("auth".equals(qop) || "auth-int".equals(qop)) {
      sb.append(nc).append(':').append(cnonce).append(':').append(qop).append(':');
    }
  }

  private static void append(StringBuilder builder, String name, String value, boolean quoted) {
    builder.append(name).append('=');
    if (quoted)
      builder.append('"').append(value).append('"');
    else
      builder.append(value);

    builder.append(", ");
  }
}
