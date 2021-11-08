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

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import io.gatling.javaapi.jdbc.JdbcDsl.*
import io.gatling.javaapi.redis.RedisDsl.*
import org.apache.commons.lang3.RandomStringUtils
import scala.Option

class FeederSampleKotlin {

  init {
//#random-mail-generator
// import org.apache.commons.lang3.RandomStringUtils
val feeder = generateSequence {
  val email = RandomStringUtils.randomAlphanumeric(20) + "@foo.com"
  mapOf("email" to email)
}.iterator()
//#random-mail-generator

//#feed
feed(feeder)
//#feed

//#strategies
// default behavior: use an Iterator on the underlying sequence
csv("foo").queue()
// randomly pick an entry in the sequence
// randomly pick an entry in the sequence
csv("foo").random()
// shuffle entries, then behave like queue
// shuffle entries, then behave like queue
csv("foo").shuffle()
// go back to the top of the sequence once the end is reached
// go back to the top of the sequence once the end is reached
csv("foo").circular()
//#strategies

//#feeder-in-memory
// using an array
arrayFeeder(arrayOf(
  mapOf("foo" to "foo1"),
  mapOf("foo" to "foo2"),
  mapOf("foo" to "foo3")
)).random()

// using a List
listFeeder(listOf(
  mapOf("foo" to "foo1"),
  mapOf("foo" to "foo2"),
  mapOf("foo" to "foo3")
)).random()
//#feeder-in-memory

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

//#eager
csv("foo.csv").eager().random()
//#eager

//#batch
// use default buffer size (2000 lines)
csv("foo.csv").batch().random()
// tune internal buffer size
csv("foo.csv").batch(200).random()
//#batch

//#unzip
csv("foo.csv.zip").unzip()
//#unzip

//#shard
csv("foo.csv").shard()
//#shard

//#json-feeders
jsonFile("foo.json")
jsonUrl("http://me.com/foo.json")
//#json-feeders

//#jdbc-feeder
// beware: you need to import the jdbc module
// import static io.gatling.javaapi.jdbc.JdbcDsl.*;

jdbcFeeder("databaseUrl", "username", "password", "SELECT * FROM users")
//#jdbc-feeder

//#sitemap-feeder
// beware: you need to import the http module
// import static io.gatling.javaapi.http.HttpDsl.*;

sitemap("/path/to/sitemap/file")
//#sitemap-feeder

//#redis-LPOP
// beware: you need to import the redis module
    val redisPool = com.redis.RedisClientPool(
      "localhost",
      6379,
      8,
      0,
      Option.apply(null),
      0,
      -1,
      3000,
      scala.Option.apply(null),
      com.redis.RedisClient.`SINGLE$`.`MODULE$`)
// use a list, so there's one single value per record, which is here named "foo"
redisFeeder(redisPool, "foo")
// identical to above, LPOP is the default
redisFeeder(redisPool, "foo").LPOP()
//#redis-LPOP


//#redis-SPOP
// read data using SPOP command from a set named "foo"
redisFeeder(redisPool, "foo").SPOP()
//#redis-SPOP

//#redis-SRANDMEMBER
// read data using SRANDMEMBER command from a set named "foo"
redisFeeder(redisPool, "foo").SRANDMEMBER()
//#redis-SRANDMEMBER

//#transform
csv("myFile.csv").transform { key, value ->
  if (key.equals("attributeThatShouldBeAnInt")) Integer.valueOf(value) else value
}
//#transform

//#records
val records = csv("myFile.csv").readRecords()
//#records
  }
}
