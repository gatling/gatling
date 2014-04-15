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
import org.specs2.specification.{ AfterExample, BeforeExample }
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import org.specs2.mock.mockito.CalledMatchers

import org.mockito.Mockito._
import org.mockito.Matchers._
import org.mockito.stubbing._
import org.mockito.invocation._

import com.redis._
import io.gatling.core.feeder.Record

/**
 * @author Ivan Mushketyk
 */
@RunWith(classOf[JUnitRunner])
class RedisFeederTest extends Specification with CalledMatchers {

  // Generate list of maps Map(<redis-key> -> <expected-valu>)
  def valsLst(key: String, s: String*): List[Record[String]] = {
    s.map(str => Map(key -> str)).toList
  }

  "redis feeder" should {
    "use lpop as default command" in {
      val clientPool = mock(classOf[RedisClientPool])
      val client = mock(classOf[RedisClient])

      // Call user specified function on withClient() call
      when(clientPool.withClient(any())).thenAnswer(new Answer[AnyRef]() {
        def answer(invocation: InvocationOnMock) = {
          val arguments = invocation.getArguments
          val func = arguments(0).asInstanceOf[Function[RedisClient, AnyRef]]
          func(client)
        }
      })

      val key = "key"
      when(client.lpop(key)).thenReturn(Some("v1"), Some("v2"), Some("v3"), None)

      val feeder = RedisFeederIterator.createIterator(clientPool, key)
      feeder.toList should be equalTo valsLst(key, "v1", "v2", "v3")
    }
  }
}
