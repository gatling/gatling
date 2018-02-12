/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.http.ahc

import io.gatling.core.session.Session

import org.asynchttpclient.channel.ChannelPoolPartitioning.PerHostChannelPoolPartitioning
import org.asynchttpclient.proxy.ProxyServer
import org.asynchttpclient.uri.Uri
import org.openjdk.jmh.annotations.Benchmark

object AhcChannelPoolPartitioningBenchmark {

  private val uri = Uri.create("https://gatling.io")

  private val session = Session("foo", 1)

  private val proxy = new ProxyServer.Builder("localhost", 80).setSecuredPort(443).build()
}

class AhcChannelPoolPartitioningBenchmark {

  import AhcChannelPoolPartitioningBenchmark._

  @Benchmark
  def ahcNoVirtualHostNoProxyHashcode(): Int =
    PerHostChannelPoolPartitioning.INSTANCE.getPartitionKey(uri, null, null).hashCode

  @Benchmark
  def gatlingNoVirtualHostNoProxyHashcode(): Int =
    new AhcChannelPoolPartitioning(session).getPartitionKey(uri, null, null).hashCode

  @Benchmark
  def ahcNoProxyHashcode(): Int =
    PerHostChannelPoolPartitioning.INSTANCE.getPartitionKey(uri, "foo", null).hashCode

  @Benchmark
  def gatlingNoProxyHashcode(): Int =
    new AhcChannelPoolPartitioning(session).getPartitionKey(uri, "foo", null).hashCode

  @Benchmark
  def ahcNoVirtualHostHashcode(): Int =
    PerHostChannelPoolPartitioning.INSTANCE.getPartitionKey(uri, null, proxy).hashCode

  @Benchmark
  def gatlingNoVirtualHostHashcode(): Int =
    new AhcChannelPoolPartitioning(session).getPartitionKey(uri, null, proxy).hashCode

  @Benchmark
  def ahcHashcode(): Int =
    PerHostChannelPoolPartitioning.INSTANCE.getPartitionKey(uri, "foo", proxy).hashCode

  @Benchmark
  def gatlingHashcode(): Int =
    new AhcChannelPoolPartitioning(session).getPartitionKey(uri, "foo", proxy).hashCode
}
