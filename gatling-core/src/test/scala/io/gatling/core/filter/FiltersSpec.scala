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
package io.gatling.core.filter

import org.scalatest.{ FlatSpecLike, Inspectors, Matchers }
import io.gatling.BaseSpec

import org.scalatest.mockito.MockitoSugar

class FiltersSpec extends FlatSpecLike with Matchers with MockitoSugar with Inspectors {

  val hosts = List(
    "http://excilys.com",
    "http://ebusinessinformation.fr",
    "http://gatling.io"
  )

  val paths = List(
    "",
    "/infos.html",
    "/assets/images/foo.png",
    "/assets/js/bar.js"
  )

  val urls = for {
    host <- hosts
    path <- paths
  } yield host + path

  val whiteList = WhiteList(List("http://excilys\\.com.*"))
  val emptyWhiteList = WhiteList()
  val blackList = BlackList(List("http://.*/assets/.*"))
  val emptyBlackList = BlackList()

  def isRequestAccepted(filters: Filters, partition: (List[String], List[String])): Unit = {
    val (expectedAccepted, expectedRejected) = partition

    forAll(expectedAccepted) {
      filters.accept(_) shouldBe true
    }
    forAll(expectedRejected) {
      filters.accept(_) shouldBe false
    }
  }

  "Filters" should "filter whitelist correctly when blacklist is empty" in {
    isRequestAccepted(Filters(whiteList, emptyBlackList), urls.partition(_.contains("excilys")))
  }

  it should "filter whitelist then blacklist when both are specified on whitefirst mode" in {
    isRequestAccepted(Filters(whiteList, blackList), urls.partition { url =>
      url.contains("excilys") && !url.contains("assets")
    })
  }

  it should "filter blacklist correctly when whitelist is empty" in {
    isRequestAccepted(Filters(blackList, emptyWhiteList), urls.partition { url =>
      !url.contains("assets")
    })
  }

  it should "filter blacklist then whitelist when both are specified on blackfirst mode" in {
    isRequestAccepted(Filters(blackList, whiteList), urls.partition { url =>
      !url.contains("assets") && url.contains("excilys")
    })
  }

  it should "filter correctly when there are multiple patterns" in {
    val patterns = List(".*foo.*", ".*bar.*")
    val url = "http://gatling.io/foo.html"

    BlackList(patterns).accept(url) shouldBe false
    WhiteList(patterns).accept(url) shouldBe true
  }

  it should "filter correctly when there are no patterns" in {
    val url = "http://gatling.io/foo.html"
    BlackList(Nil).accept(url) shouldBe true
    WhiteList(Nil).accept(url) shouldBe true
  }

  it should "be able to deal with incorrect patterns" in {
    val w = WhiteList(List("http://foo\\.com.*", "},{"))
    w.regexes should not be empty
    w.accept("http://foo.com/bar.html") shouldBe true
  }
}
