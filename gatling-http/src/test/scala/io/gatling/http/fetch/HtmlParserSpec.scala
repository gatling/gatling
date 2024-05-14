/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.http.fetch

import java.nio.charset.StandardCharsets.UTF_8

import scala.util.Using

import io.gatling.BaseSpec
import io.gatling.http.client.uri.Uri

class HtmlParserSpec extends BaseSpec {
  private val htmlContent = Using.resource(getClass.getClassLoader.getResourceAsStream("pekko.apache.org.html")) { is =>
    new String(is.readAllBytes(), UTF_8).toCharArray
  }

  private def embeddedResources(documentUri: String, htmlContent: Array[Char]) =
    new HtmlParser().getEmbeddedResources(Uri.create(documentUri), htmlContent)

  private implicit def string2Uri(string: String): Uri = Uri.create(string)

  "parsing pekko.apache.org page" should "extract all urls" in {
    embeddedResources("http://pekko.apache.org", htmlContent) shouldBe List(
      BasicResource("http://pekko.apache.org/resources/favicon.ico"),
      CssResource("http://pekko.apache.org/resources/stylesheets/style.css"),
      CssResource("http://fonts.googleapis.com/css?family=Exo:300,400,600,700"),
      CssResource("http://pekko.apache.org/resources/stylesheets/prettify-frontpage.css"),
      CssResource("http://pekko.apache.org/resources/stylesheets/base.css"),
      BasicResource("http://pekko.apache.org/resources/images/logo-small.png"),
      BasicResource("http://pekko.apache.org/resources/images/logo_dropshadow.png"),
      BasicResource("http://pekko.apache.org/resources/images/scala-sm.png"),
      BasicResource("http://pekko.apache.org/resources/images/ubs.png"),
      BasicResource("http://pekko.apache.org/resources/images/klout.png"),
      BasicResource("http://pekko.apache.org/resources/images/ign.png"),
      BasicResource("http://pekko.apache.org/resources/images/tdc.png"),
      BasicResource("http://pekko.apache.org/resources/images/vmware.png"),
      BasicResource("http://pekko.apache.org/resources/images/csc.png"),
      BasicResource("http://pekko.apache.org/resources/images/moshimonsters.png"),
      BasicResource("http://pekko.apache.org/resources/images/amazon.png"),
      BasicResource("http://pekko.apache.org/resources/images/zeebox.png"),
      BasicResource("http://pekko.apache.org/resources/images/creditsuisse.png"),
      BasicResource("http://pekko.apache.org/resources/images/autodesk.png"),
      BasicResource("http://pekko.apache.org/resources/images/atos.png"),
      BasicResource("http://pekko.apache.org/resources/images/blizzard.png"),
      BasicResource("http://pekko.apache.org/resources/images/rss.png"),
      BasicResource("http://pekko.apache.org/resources/images/watermark.png"),
      BasicResource("http://pekko.apache.org/resources/javascript/jquery.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/prettify.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/slideleft.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/jquery.livetwitter.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/livetwitter.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/jquery.rss.min.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/blogfeed.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/moment.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/dateparse.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/jquery.scrollTo-1.4.2-min.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/jquery.localscroll-1.2.7-min.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/jquery.serialScroll-1.2.2-min.js"),
      BasicResource("http://pekko.apache.org/resources/javascript/sliderbox.js")
    )
  }

  it should "ignore nested conditional comments" in {
    val html =
      s"""<!DOCTYPE html>
      <html>
        <body>
          <!--[if gt IE 5]>
          <link rel="stylesheet" type="text/css" href="style.css">
        <![endif]-->
        </body>
      </html>
      """.toCharArray

    embeddedResources("http://example.com/", html) shouldBe empty
  }

  it should "extract prefetch links" in {
    val html =
      s"""<!DOCTYPE html>
      <html>
        <head>
          <title>Test page</title>
          <link href="/resources/test-prefetch.js" rel="prefetch">
          <link href="/resources/test-prefetch2.css" rel="prefetch">
        </head>
        <body>
          <script src="/resources/app.js"></script>
        </body>
      </html>
      """.toCharArray

    embeddedResources("http://example.com/", html) shouldBe List(
      BasicResource("http://example.com/resources/test-prefetch.js"),
      CssResource("http://example.com/resources/test-prefetch2.css"),
      BasicResource("http://example.com/resources/app.js")
    )
  }
}
