/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

package io.gatling.javaapi.core

import java.{ util => ju }
import java.util.Collections

import io.gatling.core.EmptySession

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class SessionSpec extends AnyFlatSpecLike with Matchers with EmptySession {

  "getString" should "be able to return a String previously put in Scala" in {
    val scalaSession = emptySession.set("key", "value")
    val javaSession = new Session(scalaSession)
    javaSession.getString("key") shouldBe "value"
  }

  it should "be able to return a String previously put in Java" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", "value")
    javaSession.getString("key") shouldBe "value"
  }

  it should "return null on undefined key" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession)
    javaSession.getString("key") shouldBe null
  }

  it should "return null if the stored value is null" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", null)
    javaSession.getString("key") shouldBe null
  }

  "getInt" should "be able to return an Int previously put in Scala" in {
    val scalaSession = emptySession.set("key", 1)
    val javaSession = new Session(scalaSession)
    javaSession.getInt("key") shouldBe 1
  }

  it should "be able to return an Int previously put in Java" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", 1)
    javaSession.getInt("key") shouldBe 1
  }

  "getIntegerWrapper" should "return null on undefined key" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession)
    javaSession.getIntegerWrapper("key") shouldBe null
  }

  it should "return null if the stored value is null" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", null)
    javaSession.getIntegerWrapper("key") shouldBe null
  }

  "getLong" should "be able to return a Long previously put in Scala" in {
    val scalaSession = emptySession.set("key", 1L)
    val javaSession = new Session(scalaSession)
    javaSession.getLong("key") shouldBe 1L
  }

  it should "be able to return a Long previously put in Java" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", 1L)
    javaSession.getLong("key") shouldBe 1L
  }

  "getLongWrapper" should "return null on undefined key" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession)
    javaSession.getLongWrapper("key") shouldBe null
  }

  it should "return null if the stored value is null" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", null)
    javaSession.getLongWrapper("key") shouldBe null
  }

  "getDouble" should "be able to return a Double previously put in Scala" in {
    val scalaSession = emptySession.set("key", 1.1d)
    val javaSession = new Session(scalaSession)
    javaSession.getDouble("key") shouldBe 1.1d
  }

  it should "be able to return a Double previously put in Java" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", 1.1d)
    javaSession.getDouble("key") shouldBe 1.1d
  }

  "getDoubleWrapper" should "return null on undefined key" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession)
    javaSession.getDoubleWrapper("key") shouldBe null
  }

  it should "return null if the stored value is null" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", null)
    javaSession.getDoubleWrapper("key") shouldBe null
  }

  "getBoolean" should "be able to return a Boolean previously put in Scala" in {
    val scalaSession = emptySession.set("key", true)
    val javaSession = new Session(scalaSession)
    javaSession.getBoolean("key") shouldBe true
  }

  it should "be able to return a Double previously put in Java" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", true)
    javaSession.getBoolean("key") shouldBe true
  }

  "getBooleanWrapper" should "return null on undefined key" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession)
    javaSession.getBooleanWrapper("key") shouldBe null
  }

  it should "return null if the stored value is null" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", null)
    javaSession.getBooleanWrapper("key") shouldBe null
  }

  "getList" should "be able to return a Seq previously put in Scala" in {
    val scalaSession = emptySession.set("key", Seq("value"))
    val javaSession = new Session(scalaSession)

    val fetched = javaSession.getList[String]("key")
    fetched.size() shouldBe 1
    fetched.get(0) shouldBe "value"
  }

  it should "be able to return a List previously put in Java" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", Collections.singletonList("value"))

    val fetched = javaSession.getList[String]("key")
    fetched.size() shouldBe 1
    fetched.get(0) shouldBe "value"
  }

  it should "return empty on undefined key" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession)
    javaSession.getList[String]("key") shouldBe Collections.emptyList()
  }

  it should "return empty if the stored value is null" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", null)
    javaSession.getList[String]("key") shouldBe Collections.emptyList()
  }

  "getSet" should "be able to return a Set previously put in Scala" in {
    val scalaSession = emptySession.set("key", Set("value"))
    val javaSession = new Session(scalaSession)

    val fetched = javaSession.getSet[String]("key")
    fetched.size() shouldBe 1
    fetched.iterator().next() shouldBe "value"
  }

  it should "be able to return a Set previously put in Java" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", Collections.singleton("value"))

    val fetched = javaSession.getSet[String]("key")
    fetched.size() shouldBe 1
    fetched.iterator().next() shouldBe "value"
  }

  it should "return empty on undefined key" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession)
    javaSession.getSet[String]("key") shouldBe Collections.emptySet()
  }

  it should "return empty if the stored value is null" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", null)
    javaSession.getSet[String]("key") shouldBe Collections.emptySet()
  }

  "getMap" should "be able to return a Map previously put in Scala" in {
    val scalaSession = emptySession.set("key", Map("key2" -> "value"))
    val javaSession = new Session(scalaSession)

    val fetched = javaSession.getMap[String]("key")
    fetched.size() shouldBe 1
    fetched.get("key2") shouldBe "value"
  }

  it should "be able to return a Map previously put in Java" in {
    val scalaSession = emptySession
    val map = new ju.HashMap[String, Object]()
    map.put("key2", "value")
    val javaSession = new Session(scalaSession).set("key", map)

    val fetched = javaSession.getMap[String]("key")
    fetched.size() shouldBe 1
    fetched.get("key2") shouldBe "value"
  }

  it should "return empty on undefined key" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession)
    javaSession.getMap[String]("key") shouldBe Collections.emptyMap()
  }

  it should "return empty if the stored value is null" in {
    val scalaSession = emptySession
    val javaSession = new Session(scalaSession).set("key", null)
    javaSession.getMap[String]("key") shouldBe Collections.emptyMap()
  }
}
