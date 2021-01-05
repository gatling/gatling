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

package io.gatling.http.fetch

import java.nio.charset.StandardCharsets.UTF_8

import scala.util.Using

import io.gatling.BaseSpec
import io.gatling.commons.util.Io._
import io.gatling.http.client.uri.Uri

class HtmlParserSpec extends BaseSpec {

  private val htmlContent = Using.resource(getClass.getClassLoader.getResourceAsStream("akka.io.html")) {
    _.toString(UTF_8).toCharArray
  }

  private def embeddedResources(documentUri: String, htmlContent: Array[Char]) =
    new HtmlParser().getEmbeddedResources(Uri.create(documentUri), htmlContent)

  private implicit def string2Uri(string: String): Uri = Uri.create(string)

  "parsing akka.io page" should "extract all urls" in {
    embeddedResources("http://akka.io", htmlContent) shouldBe List(
      BasicResource("http://akka.io/resources/favicon.ico"),
      CssResource("http://akka.io/resources/stylesheets/style.css"),
      CssResource("http://fonts.googleapis.com/css?family=Exo:300,400,600,700"),
      CssResource("http://akka.io/resources/stylesheets/prettify-frontpage.css"),
      CssResource("http://akka.io/resources/stylesheets/base.css"),
      BasicResource("http://akka.io/resources/images/logo-small.png"),
      BasicResource("http://akka.io/resources/images/logo_dropshadow.png"),
      BasicResource("http://akka.io/resources/images/scala-sm.png"),
      BasicResource("http://akka.io/resources/images/ubs.png"),
      BasicResource("http://akka.io/resources/images/klout.png"),
      BasicResource("http://akka.io/resources/images/ign.png"),
      BasicResource("http://akka.io/resources/images/tdc.png"),
      BasicResource("http://akka.io/resources/images/vmware.png"),
      BasicResource("http://akka.io/resources/images/csc.png"),
      BasicResource("http://akka.io/resources/images/moshimonsters.png"),
      BasicResource("http://akka.io/resources/images/amazon.png"),
      BasicResource("http://akka.io/resources/images/zeebox.png"),
      BasicResource("http://akka.io/resources/images/creditsuisse.png"),
      BasicResource("http://akka.io/resources/images/autodesk.png"),
      BasicResource("http://akka.io/resources/images/atos.png"),
      BasicResource("http://akka.io/resources/images/blizzard.png"),
      BasicResource("http://akka.io/resources/images/rss.png"),
      BasicResource("http://akka.io/resources/images/watermark.png"),
      BasicResource("http://akka.io/resources/javascript/jquery.js"),
      BasicResource("http://akka.io/resources/javascript/prettify.js"),
      BasicResource("http://akka.io/resources/javascript/slideleft.js"),
      BasicResource("http://akka.io/resources/javascript/jquery.livetwitter.js"),
      BasicResource("http://akka.io/resources/javascript/livetwitter.js"),
      BasicResource("http://akka.io/resources/javascript/jquery.rss.min.js"),
      BasicResource("http://akka.io/resources/javascript/blogfeed.js"),
      BasicResource("http://akka.io/resources/javascript/moment.js"),
      BasicResource("http://akka.io/resources/javascript/dateparse.js"),
      BasicResource("http://akka.io/resources/javascript/jquery.scrollTo-1.4.2-min.js"),
      BasicResource("http://akka.io/resources/javascript/jquery.localscroll-1.2.7-min.js"),
      BasicResource("http://akka.io/resources/javascript/jquery.serialScroll-1.2.2-min.js"),
      BasicResource("http://akka.io/resources/javascript/sliderbox.js")
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
}
