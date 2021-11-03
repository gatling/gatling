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

import io.gatling.core.Predef._

class FeederSampleScala {

  {
//#random-mail-generator
import scala.util.Random
val feeder = Iterator.continually {
  Map("email" -> s"${Random.alphanumeric.take(20).mkString}@foo.com")
}
//#random-mail-generator

//#feed
feed(feeder)
//#feed

//#strategies
// default behavior: use an Iterator on the underlying sequence
csv("foo").queue()
// randomly pick an entry in the sequence
csv("foo").random()
// shuffle entries, then behave like queue
csv("foo").shuffle()
// go back to the top of the sequence once the end is reached
csv("foo").circular()
//#strategies
  }

  {
//#feeder-in-memory
// using an array (implicit conversion)
Array(
  Map("foo" -> "foo1", "bar" -> "bar1"),
  Map("foo" -> "foo2", "bar" -> "bar2"),
  Map("foo" -> "foo3", "bar" -> "bar3")
).random

// using a IndexedSeq (implicit conversion)
IndexedSeq(
  Map("foo" -> "foo1", "bar" -> "bar1"),
  Map("foo" -> "foo2", "bar" -> "bar2"),
  Map("foo" -> "foo3", "bar" -> "bar3")
).random
//#feeder-in-memory
  }

  {
//#sep-values-feeders
// use a comma separator
csv("foo.csv")
// use a tabulation separator
tsv("foo.tsv")
// use a semicolon separator
ssv("foo.ssv")
// use a custom separator
separatedValues("foo.txt", '#')
//#sep-values-feeders
  }

  {
//#eager
csv("foo.csv").eager
//#eager
  }

  {
//#batch
// use default buffer size (2000 lines)
csv("foo.csv").batch
// tune internal buffer size
csv("foo.csv").batch(200)
//#batch
  }

  {
//#unzip
csv("foo.csv.zip").unzip
//#unzip
  }

  {
//#shard
csv("foo.csv").shard
//#shard
  }

  {
//#json-feeders
jsonFile("foo.json")
jsonUrl("http://me.com/foo.json")
//#json-feeders
  }

  {
//#jdbc-feeder
// beware: you need to import the jdbc module
import io.gatling.jdbc.Predef._

jdbcFeeder("databaseUrl", "username", "password", "SELECT * FROM users")
//#jdbc-feeder
  }

  {
//#sitemap-feeder
// beware: you need to import the http module
import io.gatling.http.Predef._

sitemap("/path/to/sitemap/file")
//#sitemap-feeder
  }

  {
//#redis-LPOP
import io.gatling.redis.Predef._

import com.redis._

val redisPool = new RedisClientPool("localhost", 6379)

// use a list, so there's one single value per record, which is here named "foo"
// same as redisFeeder(redisPool, "foo").LPOP
redisFeeder(redisPool, "foo")
//#redis-LPOP
  }

  {
import io.gatling.redis.Predef._

import com.redis._

val redisPool = new RedisClientPool("localhost", 6379)

//#redis-SPOP
// read data using SPOP command from a set named "foo"
redisFeeder(redisPool, "foo").SPOP
//#redis-SPOP
  }

  {
import io.gatling.redis.Predef._

import com.redis._

val redisPool = new RedisClientPool("localhost", 6379)

//#redis-SRANDMEMBER
// read data using SRANDMEMBER command from a set named "foo"
redisFeeder(redisPool, "foo").SRANDMEMBER
//#redis-SRANDMEMBER
  }

  {
//#transform
csv("myFile.csv").transform {
  case ("attributeThatShouldBeAnInt", string) => string.toInt
}
//#transform
  }

  {
//#records
val records: Seq[Map[String, Any]] = csv("myFile.csv").readRecords
//#records
  }
}
