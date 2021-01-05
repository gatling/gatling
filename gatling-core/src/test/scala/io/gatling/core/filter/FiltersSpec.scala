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

package io.gatling.core.filter

import org.scalatest.Inspectors
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

class FiltersSpec extends AnyFlatSpec with Matchers with MockitoSugar with Inspectors {

  private val hosts = List(
    "http://takima.fr",
    "http://ebusinessinformation.fr",
    "https://gatling.io"
  )

  private val paths = List(
    "",
    "/infos.html",
    "/assets/images/foo.png",
    "/assets/js/bar.js"
  )

  private val urls = for {
    host <- hosts
    path <- paths
  } yield host + path

  private val whiteList = new WhiteList(List("http://takima\\.fr.*"))
  private val blackList = new BlackList(List("http[s]?://.*/assets/.*"))

  private def checkRequestAccepted(filters: Filters, partition: (List[String], List[String])): Unit = {
    val (expectedAccepted, expectedRejected) = partition

    forAll(expectedAccepted) {
      filters.accept(_) shouldBe true
    }
    forAll(expectedRejected) {
      filters.accept(_) shouldBe false
    }
  }

  "Filters" should "filter whitelist correctly when blacklist is empty" in {
    checkRequestAccepted(new Filters(whiteList, BlackList.Empty), urls.partition(_.contains("takima")))
  }

  it should "filter whitelist then blacklist when both are specified on whitefirst mode" in {
    checkRequestAccepted(
      new Filters(whiteList, blackList),
      urls.partition { url =>
        url.contains("takima") && !url.contains("assets")
      }
    )
  }

  it should "filter blacklist correctly when whitelist is empty" in {
    checkRequestAccepted(
      new Filters(blackList, WhiteList.Empty),
      urls.partition { url =>
        !url.contains("assets")
      }
    )
  }

  it should "filter blacklist then whitelist when both are specified on blackfirst mode" in {
    checkRequestAccepted(
      new Filters(blackList, whiteList),
      urls.partition { url =>
        !url.contains("assets") && url.contains("takima")
      }
    )
  }

  it should "filter correctly when there are multiple patterns" in {
    val patterns = List(".*foo.*", ".*bar.*")
    val url = "https://gatling.io/foo.html"

    new BlackList(patterns).accept(url) shouldBe false
    new WhiteList(patterns).accept(url) shouldBe true
  }

  it should "filter correctly when there are no patterns" in {
    val url = "https://gatling.io/foo.html"
    new BlackList(Nil).accept(url) shouldBe true
    new WhiteList(Nil).accept(url) shouldBe true
  }

  it should "be able to deal with incorrect patterns" in {
    val w = new WhiteList(List("http://foo\\.com.*", "},{"))
    w.regexes should not be empty
    w.accept("http://foo.com/bar.html") shouldBe true
  }
}
