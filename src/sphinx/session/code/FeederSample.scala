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

class FeederSample {

  {
    //#random-mail-generator
    import scala.util.Random
    val feeder = Iterator.continually(Map("email" -> (Random.alphanumeric.take(20).mkString + "@foo.com")))
    //#random-mail-generator

    //#feed
    feed(feeder)
    //#feed

    //#feed-multiple
    feed(feeder, 2)
    //#feed-multiple

    csv("foo")
    //#strategies
    .queue // default behavior: use an Iterator on the underlying sequence
    .random // randomly pick an entry in the sequence
    .shuffle // shuffle entries, then behave like queue
    .circular // go back to the top of the sequence once the end is reached
    //#strategies
  }

  {
    //#feeder-from-array-with-random
    val feeder = Array(
      Map("foo" -> "foo1", "bar" -> "bar1"),
      Map("foo" -> "foo2", "bar" -> "bar2"),
      Map("foo" -> "foo3", "bar" -> "bar3")
    ).random
    //#feeder-from-array-with-random
  }

  {
    //#sep-values-feeders
    val csvFeeder = csv("foo.csv") // use a comma separator
    val tsvFeeder = tsv("foo.tsv") // use a tabulation separator
    val ssvFeeder = ssv("foo.ssv") // use a semicolon separator
    val customSeparatorFeeder = separatedValues("foo.txt", '#') // use your own separator
    //#sep-values-feeders
  }

  {
    //#eager
    val csvFeeder = csv("foo.csv").eager.random
    //#eager
  }

  {
    //#batch
    val csvFeeder = csv("foo.csv").batch.random
    val csvFeeder2 = csv("foo.csv").batch(200).random // tune internal buffer size
    //#batch
  }

  {
    //#unzip
    val csvFeeder = csv("foo.csv.zip").unzip
    //#unzip
  }

  {
    //#shard
    val csvFeeder = csv("foo.csv.zip").shard
    //#shard
  }

  {
    //#json-feeders
    val jsonFileFeeder = jsonFile("foo.json")
    val jsonUrlFeeder = jsonUrl("http://me.com/foo.json")
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

    val feeder = sitemap("/path/to/sitemap/file")
    //#sitemap-feeder
  }

  {
    //#redis-LPOP
    import io.gatling.redis.Predef._

    import com.redis._

    val redisPool = new RedisClientPool("localhost", 6379)

    // use a list, so there's one single value per record, which is here named "foo"
    // same as redisFeeder(redisPool, "foo").LPOP
    val feeder = redisFeeder(redisPool, "foo")
    //#redis-LPOP
  }

  {
    import io.gatling.redis.Predef._

    import com.redis._

    val redisPool = new RedisClientPool("localhost", 6379)

    //#redis-SPOP
    // read data using SPOP command from a set named "foo"
    val feeder = redisFeeder(redisPool, "foo").SPOP
    //#redis-SPOP
  }

  {
    import io.gatling.redis.Predef._

    import com.redis._

    val redisPool = new RedisClientPool("localhost", 6379)

    //#redis-SRANDMEMBER
    // read data using SRANDMEMBER command from a set named "foo"
    val feeder = redisFeeder(redisPool, "foo").SRANDMEMBER
    //#redis-SRANDMEMBER
  }

  {
    //#redis-1million
    import java.io.{ File, PrintWriter }

    import io.gatling.redis.util.RedisHelper._

    def generateOneMillionUrls(): Unit = {
      val writer = new PrintWriter(new File("/tmp/loadtest.txt"))
      try {
        for (i <- 0 to 1000000) {
          val url = "test?id=" + i
          // note the list name "URLS" here
          writer.write(generateRedisProtocol("LPUSH", "URLS", url))
        }
      } finally {
        writer.close()
      }
    }
    //#redis-1million

    generateOneMillionUrls()
  }

  {
    //#convert
    csv("myFile.csv").convert {
      case ("attributeThatShouldBeAnInt", string) => string.toInt
    }
    //#convert
  }

  {
    //#records
    val records: Seq[Map[String, Any]] = csv("myFile.csv").readRecords
    //#records
  }

  {
    //#non-shared
    val records = csv("foo.csv").readRecords

    foreach(records, "record") {
      exec(flattenMapIntoAttributes("${record}"))
    }
    //#non-shared
  }

  {
    //#user-dependent-data
    import java.util.concurrent.ThreadLocalRandom

    import io.gatling.core.feeder._

    // index records by project
    val issuesByProject: Map[String, Seq[Any]] =
      csv("projectIssue.csv").readRecords
        .groupMap(record => record("project").toString)(record => record("issue"))

    // inject project
    feed(csv("userProject.csv"))
      .exec { session =>
        // fetch project from  session
        session("project").validate[String].map { project =>
          // fetch project's issues
          val issues = issuesByProject(project)

          // randomly select an issue
          val selectedIssue = issues(ThreadLocalRandom.current.nextInt(issues.length))

          // inject the issue in the session
          session.set("issue", selectedIssue)
        }
      }
    //#user-dependent-data
  }
}
