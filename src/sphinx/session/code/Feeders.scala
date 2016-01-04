/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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
import io.gatling.jdbc.Predef._
import io.gatling.http.Predef._

class Feeders {

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
      .queue    // default behavior: use an Iterator on the underlying sequence
      .random   // randomly pick an entry in the sequence
      .shuffle  // shuffle entries, then behave live queue
      .circular // go back to the top of the sequence once the end is reached
      //#strategies
  }

  {
    //#feeder-from-array-with-random
    val feeder = Array(
      Map("foo" -> "foo1", "bar" -> "bar1"),
      Map("foo" -> "foo2", "bar" -> "bar2"),
      Map("foo" -> "foo3", "bar" -> "bar3")).random
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
    //#escape-char
    val csvFeeder = csv("foo.csv", escapeChar = '\\')
    //#escape-char
  }

  {
    //#json-feeders
    val jsonFileFeeder = jsonFile("foo.json")
    val jsonUrlFeeder = jsonUrl("http://me.com/foo.json")
    //#json-feeders
  }

  {
    //#jdbc-feeder
    jdbcFeeder("databaseUrl", "username", "password", "SELECT * FROM users")
    //#jdbc-feeder
  }

  {
    //#sitemap-feeder
    val feeder = sitemap("/path/to/sitemap/file")
    //#sitemap-feeder
  }

  {
    //#redis-LPOP
    import com.redis._
    import io.gatling.redis.feeder.RedisFeeder

    val redisPool = new RedisClientPool("localhost", 6379)

    // use a list, so there's one single value per record, which is here named "foo"
    val feeder = RedisFeeder(redisPool, "foo")
    //#redis-LPOP
  }

  {
    import com.redis._
    import io.gatling.redis.feeder.RedisFeeder

    val clientPool = new RedisClientPool("localhost", 6379)

    //#redis-SPOP
    // read data using SPOP command from a set named "foo"
    val feeder = RedisFeeder(clientPool, "foo", RedisFeeder.SPOP)
    //#redis-SPOP
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
    //#non-shared
    val records = csv("foo.csv").records

    foreach(records, "record") {
      exec(flattenMapIntoAttributes("${record}"))
    }
    //#non-shared
  }

  {
    //#user-dependent-data
    import io.gatling.core.feeder._
    import scala.concurrent.forkjoin.ThreadLocalRandom

    // index records by project
    val recordsByProject: Map[String, IndexedSeq[Record[String]]] =
      csv("projectIssue.csv").records.groupBy{ record => record("project") }

    // convert the Map values to get only the issues instead of the full records
    val issuesByProject: Map[String, IndexedSeq[String]] =
      recordsByProject.mapValues{ records => records.map {record => record("issue")} }

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
