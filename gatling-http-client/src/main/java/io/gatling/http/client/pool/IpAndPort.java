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

package io.gatling.http.client.pool;

import java.util.Arrays;
import java.util.Objects;

public class IpAndPort {

  private final byte[] ip;
  private final int port;

  public IpAndPort(byte[] ip, int port) {
    this.ip = ip;
    this.port = port;
  }

  public byte[] getIp() {
    return ip;
  }

  public int getPort() {
    return port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IpAndPort ipAndPort = (IpAndPort) o;
    return port == ipAndPort.port &&
      Arrays.equals(ip, ipAndPort.ip);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(port);
    result = 31 * result + Arrays.hashCode(ip);
    return result;
  }

  @Override
  public String toString() {
    return "IpAndPort{" +
      "ip=" + Arrays.toString(ip) +
      ", port=" + port +
      '}';
  }
}
