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
package io.gatling.http.cache

import org.junit.runner.RunWith

import org.specs2.mock.Mockito
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.{ Before, Specification }

import io.gatling.core.session.Session
import io.gatling.http.MockUtils

import java.net.URI

/**
 * @author Ivan Mushketyk
 */
@RunWith(classOf[JUnitRunner])
class PermanentRedirectSpec extends Specification with Mockito {

  class Context extends Before {
    var session = Session("mockSession", "mockUserName")

    def addRedirect(from: String, to: String): Unit =
      session = PermanentRedirect.addRedirect(session, new URI(from), new URI(to))

    def before() {}
  }
  "redirect memoization" should {
    "return transaction with no redirect cache" in new Context {
      val tx = MockUtils.txTo("http://example.com/", session)
      val actualTx = PermanentRedirect.getRedirect(tx)

      actualTx should be equalTo tx
    }

    "redirect memoization should be empty" in new Context {
      CacheHandling.getRedirectMemoizationStore(session) should be empty
    }

    "return updated transaction with single redirect" in new Context {
      addRedirect("http://example.com/", "http://gatling-tool.org/")

      val origTx = MockUtils.txTo("http://example.com/", session)
      val tx = PermanentRedirect.getRedirect(origTx)

      tx.request.getURI should be equalTo new URI("http://gatling-tool.org/")
      tx.redirectCount should be equalTo 1

    }

    "return updated transaction with several redirects" in new Context {
      addRedirect("http://example.com/", "http://gatling-tool.org/")
      addRedirect("http://gatling-tool.org/", "http://gatling-tool2.org/")
      addRedirect("http://gatling-tool2.org/", "http://gatling-tool3.org/")

      val origTx = MockUtils.txTo("http://example.com/", session)
      val tx = PermanentRedirect.getRedirect(origTx)

      tx.request.getURI should be equalTo new URI("http://gatling-tool3.org/")
      tx.redirectCount should be equalTo 3

    }

    "return updated transaction with several redirects" in new Context {
      addRedirect("http://example.com/", "http://gatling-tool.org/")
      addRedirect("http://gatling-tool.org/", "http://gatling-tool2.org/")
      addRedirect("http://gatling-tool2.org/", "http://gatling-tool3.org/")

      // Redirect count is already 2
      val origTx = MockUtils.txTo("http://example.com/", session, 2)
      val tx = PermanentRedirect.getRedirect(origTx)

      tx.request.getURI should be equalTo new URI("http://gatling-tool3.org/")
      // After 3 more redirects it is now equal to 5
      tx.redirectCount should be equalTo 5

    }
  }
}
