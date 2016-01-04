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
package io.gatling.http.fetch

import scala.io.Codec.UTF8
import scala.io.Source

import io.gatling.BaseSpec
import io.gatling.commons.util.Io._

import org.asynchttpclient.uri.Uri

class HtmlParserSpec extends BaseSpec {

  val htmlContent = withCloseable(getClass.getClassLoader.getResourceAsStream("akka.io.html")) { is =>
    Source.fromInputStream(is)(UTF8).getLines().mkString
  }

  def mockHtml(body: String): String =
    s"""<!DOCTYPE html>
      <html>
        <body>
          $body
        </body>
      </html>
      """

  def embeddedResources(documentUri: String, htmlContent: String, userAgent: Option[UserAgent]) =
    new HtmlParser().getEmbeddedResources(Uri.create(documentUri), htmlContent, userAgent)

  implicit def string2URI(string: String) = Uri.create(string)

  "parsing Akka.io page" should "extract all urls" in {
    embeddedResources("http://akka.io", htmlContent, None) shouldBe List(
      RegularResource("http://akka.io/resources/favicon.ico"),
      CssResource("http://akka.io/resources/stylesheets/style.css"),
      CssResource("http://fonts.googleapis.com/css?family=Exo:300,400,600,700"),
      CssResource("http://akka.io/resources/stylesheets/prettify-frontpage.css"),
      CssResource("http://akka.io/resources/stylesheets/base.css"),
      RegularResource("http://akka.io/resources/images/logo-small.png"),
      RegularResource("http://akka.io/resources/images/logo_dropshadow.png"),
      RegularResource("http://akka.io/resources/images/scala-sm.png"),
      RegularResource("http://akka.io/resources/images/ubs.png"),
      RegularResource("http://akka.io/resources/images/klout.png"),
      RegularResource("http://akka.io/resources/images/ign.png"),
      RegularResource("http://akka.io/resources/images/tdc.png"),
      RegularResource("http://akka.io/resources/images/vmware.png"),
      RegularResource("http://akka.io/resources/images/csc.png"),
      RegularResource("http://akka.io/resources/images/moshimonsters.png"),
      RegularResource("http://akka.io/resources/images/amazon.png"),
      RegularResource("http://akka.io/resources/images/zeebox.png"),
      RegularResource("http://akka.io/resources/images/creditsuisse.png"),
      RegularResource("http://akka.io/resources/images/autodesk.png"),
      RegularResource("http://akka.io/resources/images/atos.png"),
      RegularResource("http://akka.io/resources/images/blizzard.png"),
      RegularResource("http://akka.io/resources/images/rss.png"),
      RegularResource("http://akka.io/resources/images/watermark.png"),
      RegularResource("http://akka.io/resources/javascript/jquery.js"),
      RegularResource("http://akka.io/resources/javascript/prettify.js"),
      RegularResource("http://akka.io/resources/javascript/slideleft.js"),
      RegularResource("http://akka.io/resources/javascript/jquery.livetwitter.js"),
      RegularResource("http://akka.io/resources/javascript/livetwitter.js"),
      RegularResource("http://akka.io/resources/javascript/jquery.rss.min.js"),
      RegularResource("http://akka.io/resources/javascript/blogfeed.js"),
      RegularResource("http://akka.io/resources/javascript/moment.js"),
      RegularResource("http://akka.io/resources/javascript/dateparse.js"),
      RegularResource("http://akka.io/resources/javascript/jquery.scrollTo-1.4.2-min.js"),
      RegularResource("http://akka.io/resources/javascript/jquery.localscroll-1.2.7-min.js"),
      RegularResource("http://akka.io/resources/javascript/jquery.serialScroll-1.2.2-min.js"),
      RegularResource("http://akka.io/resources/javascript/sliderbox.js")
    )
  }

  it should "extract IE css" in {
    val html = mockHtml(
      """
          <!--[if IE 9]>
            <link rel="stylesheet" type="text/css" href="style.css">
          <![endif]-->
      """
    )

    embeddedResources("http://example.com/", html, Some(UserAgent(UserAgent.IE, 9))) shouldBe List(CssResource("http://example.com/style.css"))
  }

  it should "not extract IE css" in {
    val html = mockHtml(
      """
        <!--[if IE 6]>
          <link rel="stylesheet" type="text/css" href="style.css">
        <![endif]-->
      """
    )

    embeddedResources("http://example.com/", html, Some(UserAgent(UserAgent.IE, 9))) shouldBe empty
  }

  it should "extract style for IE 7" in {
    val html = mockHtml(
      """
        <!--[if IE 6]>
          <link rel="stylesheet" type="text/css" href="style6.css">
        <![endif]-->
        <!--[if IE 7]>
          <link rel="stylesheet" type="text/css" href="style7.css">
        <![endif]-->
      """
    )

    embeddedResources("http://example.com/", html, Some(UserAgent(UserAgent.IE, 7))) shouldBe List(CssResource("http://example.com/style7.css"))
  }

  it should "parse nexted conditional comments" in {
    val html = mockHtml(
      """
        <!--[if gt IE 6]>
          <!--[if lte IE 8]>
            <!--[if lte IE 7]>
              <link rel="stylesheet" type="text/css" href="style7.css">
            <![endif]-->

            <link rel="stylesheet" type="text/css" href="style8.css">
          <![endif]-->

          <link rel="stylesheet" type="text/css" href="style9.css">
        <![endif]-->
      """
    )

    embeddedResources("http://example.com/", html, Some(UserAgent(UserAgent.IE, 9))) shouldBe List(CssResource("http://example.com/style9.css"))

    embeddedResources("http://example.com/", html, Some(UserAgent(UserAgent.IE, 8))) shouldBe List(CssResource("http://example.com/style8.css"), CssResource("http://example.com/style9.css"))
  }

  it should "parse nested conditional comments with alternative syntax" in {
    val html = mockHtml(
      """
        <!--[if gt IE 5]>
        <![if lt IE 6]>
          <link rel="stylesheet" type="text/css" href="style55.css">
        <![endif]
        <![endif]-->
      """
    )

    embeddedResources("http://example.com/", html, Some(UserAgent(UserAgent.IE, 5.5f))) shouldBe List(CssResource("http://example.com/style55.css"))

    embeddedResources("http://example.com/", html, Some(UserAgent(UserAgent.IE, 6))) shouldBe empty
  }

  it should "ignore nested conditional comments for None UserAgent" in {
    val html = mockHtml(
      """
        <!--[if gt IE 5]>
          <link rel="stylesheet" type="text/css" href="style.css">
        <![endif]-->
      """
    )

    embeddedResources("http://example.com/", html, None) shouldBe empty
  }

  it should "ignore nested conditional comments for user agents other than MSIE" in {
    val html = mockHtml(
      """
        <!--[if gt IE 5]>
          <link rel="stylesheet" type="text/css" href="style.css">
        <![endif]-->
      """
    )

    embeddedResources("http://example.com/", html, Some(UserAgent("Firefox", 29.0f))) shouldBe empty
  }
}
