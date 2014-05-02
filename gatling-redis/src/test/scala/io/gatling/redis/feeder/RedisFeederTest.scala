/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.redis.feeder

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import org.specs2.mutable.Before
import org.specs2.mock.mockito.CalledMatchers

import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.stubbing._
import org.mockito.invocation._

import com.redis._
import io.gatling.core.feeder.Record
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.akka.GatlingActorSystem
import org.specs2.specification.BeforeExample
import io.gatling.core.test.ActorSupport

/**
 * @author Ivan Mushketyk
 */
@RunWith(classOf[JUnitRunner])
class RedisFeederTest extends Specification with CalledMatchers {

  val KEY = "key"

  // Generate list of maps Map(<redis-key> -> <expected-value>)
  def valsLst(key: String, s: String*): List[Record[String]] = {
    s.map(str => Map(key -> str)).toList
  }

  trait MockContext extends Before {
    var clientPool: RedisClientPool = _
    var client: RedisClient = _

    def before() {
      clientPool = mock(classOf[RedisClientPool])
      client = mock(classOf[RedisClient])

      // Call user specified function on withClient() call
      when(clientPool.withClient(any())).thenAnswer(new Answer[AnyRef]() {
        def answer(invocation: InvocationOnMock) = {
          val arguments = invocation.getArguments
          val func = arguments(0).asInstanceOf[Function[RedisClient, AnyRef]]
          func(client)
        }
      })
    }
  }

  "redis feeder" should {
    "use lpop as default command" in ActorSupport.of{
      new MockContext {

        when(client.lpop(KEY)).thenReturn(Some("v1"), Some("v2"), Some("v3"), None)

        val feeder = RedisFeeder(clientPool, KEY)
        val actual = feeder.toList
        actual should be equalTo valsLst(KEY, "v1", "v2", "v3")
      }
    }

    "use spop command" in ActorSupport.of{
      new MockContext {

        when(client.spop(KEY)).thenReturn(Some("v1"), Some("v2"), Some("v3"), None)

        val feeder = RedisFeeder(clientPool, KEY, RedisFeeder.SPOP)
        val actual = feeder.toList
        actual should be equalTo valsLst(KEY, "v1", "v2", "v3")
      }
    }

    "use srandmember command" in ActorSupport.of{
      new MockContext {

        when(client.srandmember(KEY)).thenReturn(Some("v1"), Some("v2"), Some("v3"))

        val feeder = RedisFeeder(clientPool, KEY, RedisFeeder.SRANDMEMBER)
        feeder.next() should be equalTo Map(KEY -> "v1")
        feeder.next() should be equalTo Map(KEY -> "v2")
        feeder.next() should be equalTo Map(KEY -> "v3")
      }
    }
  }
}
