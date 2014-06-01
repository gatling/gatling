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
package io.gatling.http.fetch

import java.net.URI

import scala.io.Codec.UTF8

import org.junit.runner.RunWith

import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.dongxiguo.fastring.Fastring.Implicits._

import io.gatling.core.util.IO._
import io.gatling.http.fetch.HtmlParser._

@RunWith(classOf[JUnitRunner])
class HtmlParserSpec extends Specification {

  "html parser" should {

    val htmlContent = withCloseable(getClass.getClassLoader.getResourceAsStream("akka.io.html")) {
      _.toCharArray(UTF8.charSet)
    }

      def mockHtml(body: String): Array[Char] = {
        fast"""<!DOCTYPE html>
      <html>
        <body>
          ${body}
        </body>
      </html>
      """.toString.toCharArray
      }

      implicit def string2URI(string: String) = URI.create(string)

    "extract all urls from akka.io page" in {
      HtmlParser.getEmbeddedResources(new URI("http://akka.io"), htmlContent) must beEqualTo(List(
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
        RegularResource("http://akka.io/resources/javascript/sliderbox.js")))
    }

    "extract IE css" in {
      val html = mockHtml(
        """
          <!--[if IE 9]>
            <link rel="stylesheet" type="text/css" href="style.css">
          <![endif]-->
        """)

      HtmlParser.getEmbeddedResources(new URI("http://example.com/"), html, Some(Browser(ConditionalComment.IE, 9))) must beEqualTo(
        List(CssResource("http://example.com/style.css")))
    }

    "not extract IE css" in {
      val html = mockHtml(
        """
          <!--[if IE 6]>
            <link rel="stylesheet" type="text/css" href="style.css">
          <![endif]-->
        """)

      HtmlParser.getEmbeddedResources(new URI("http://example.com/"), html, Some(Browser(ConditionalComment.IE, 9))) must beEqualTo(
        Nil)
    }

    "extract style for IE 7" in {
      val html = mockHtml(
        """
          <!--[if IE 6]>
            <link rel="stylesheet" type="text/css" href="style6.css">
          <![endif]-->
          <!--[if IE 7]>
            <link rel="stylesheet" type="text/css" href="style7.css">
          <![endif]-->
        """)

      HtmlParser.getEmbeddedResources(new URI("http://example.com/"), html, Some(Browser(ConditionalComment.IE, 7))) must beEqualTo(
        List(CssResource("http://example.com/style7.css")))
    }

    "parse nexted conditional comments" in {
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
        """)

      HtmlParser.getEmbeddedResources(new URI("http://example.com/"), html, Some(Browser(ConditionalComment.IE, 9))) must beEqualTo(
        List(CssResource("http://example.com/style9.css")))

      HtmlParser.getEmbeddedResources(new URI("http://example.com/"), html, Some(Browser(ConditionalComment.IE, 8))) must beEqualTo(
        List(
          CssResource("http://example.com/style8.css"),
          CssResource("http://example.com/style9.css")))
    }

    "parse nested conditional comments with alternative syntax" in {
      val html = mockHtml(
        """
          <!--[if gt IE 5]>
          <![if lt IE 6]>
            <link rel="stylesheet" type="text/css" href="style55.css">
          <![endif]
          <![endif]-->
        """)

      HtmlParser.getEmbeddedResources(new URI("http://example.com/"), html, Some(Browser(ConditionalComment.IE, 5.5))) must beEqualTo(
        List(CssResource("http://example.com/style55.css")))

      HtmlParser.getEmbeddedResources(new URI("http://example.com/"), html, Some(Browser(ConditionalComment.IE, 6))) must beEqualTo(
        Nil)
    }
  }
}
