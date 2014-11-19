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
package io.gatling.recorder.scenario.template

import org.scalatest.{ FlatSpec, Matchers }

import io.gatling.recorder.scenario.RequestElement

class ExtractedUrisSpec extends FlatSpec with Matchers {

  val GATLING_URL1 = "http://gatling-tool.org/path1/file1"
  val GATLING_URL2 = "http://gatling-tool.org/path1/file2"

  val EXAMPLE_URL1 = "http://example.com/path/file1"
  val EXAMPLE_URL2 = "http://example.com/path/file2"

  def mockRequestElement(uri: String) = RequestElement(uri, "get", Map(), None, None, 200, Nil)

  def extractUris(uris: Seq[String]): ExtractedUris = {
    val requestElements = uris.map(mockRequestElement)
    new ExtractedUris(requestElements)
  }

  "extracting uris" should "extract common root" in {
    val extractedUris = extractUris(Seq(GATLING_URL1, GATLING_URL2))

    extractedUris.vals shouldBe List(Value("uri1", "http://gatling-tool.org/path1"))
    extractedUris.renderUri(GATLING_URL1).toString shouldBe "uri1 + \"\"\"/file1\"\"\""
    extractedUris.renderUri(GATLING_URL2).toString shouldBe "uri1 + \"\"\"/file2\"\"\""
  }

  it should "extract common root from different authorities" in {
    val extractedUris = extractUris(Seq(GATLING_URL1, GATLING_URL2, EXAMPLE_URL1, EXAMPLE_URL2))

    extractedUris.vals shouldBe List(Value("uri2", "http://gatling-tool.org/path1"), Value("uri1", "http://example.com/path"))

    extractedUris.renderUri(GATLING_URL1).toString shouldBe "uri2 + \"\"\"/file1\"\"\""
    extractedUris.renderUri(GATLING_URL2).toString shouldBe "uri2 + \"\"\"/file2\"\"\""
    extractedUris.renderUri(EXAMPLE_URL1).toString shouldBe "uri1 + \"\"\"/file1\"\"\""
    extractedUris.renderUri(EXAMPLE_URL2).toString shouldBe "uri1 + \"\"\"/file2\"\"\""
  }

  it should "extract only authorities" in {
    val extractedUris = extractUris(Seq(GATLING_URL1, EXAMPLE_URL1))

    extractedUris.vals shouldBe List(Value("uri2", "gatling-tool.org"), Value("uri1", "example.com"))

    extractedUris.renderUri(GATLING_URL1).toString shouldBe "\"http://\" + uri2 + \"\"\"/path1/file1\"\"\""
    extractedUris.renderUri(EXAMPLE_URL1).toString shouldBe "\"http://\" + uri1 + \"\"\"/path/file1\"\"\""
  }

  it should "extract authorities" in {
    val gatlingUri = "http://gatling-tool.org/?q=v"
    val exampleUri = "http://example.com/?var=val"

    val extractedUris = extractUris(Seq(gatlingUri, exampleUri))

    extractedUris.vals shouldBe List(Value("uri2", "gatling-tool.org"), Value("uri1", "example.com"))

    extractedUris.renderUri(gatlingUri).toString shouldBe "\"http://\" + uri2 + \"\"\"/?q=v\"\"\""
    extractedUris.renderUri(exampleUri).toString shouldBe "\"http://\" + uri1 + \"\"\"/?var=val\"\"\""
  }

  it should "preserve the protocol" in {
    val gatlingUri = "https://gatling-tool.org/?q=v"
    val extractedUris = extractUris(Seq(gatlingUri))

    extractedUris.vals shouldBe List(Value("uri1", "gatling-tool.org"))
    extractedUris.renderUri(gatlingUri).toString shouldBe "\"https://\" + uri1 + \"\"\"/?q=v\"\"\""
  }

  it should "port and user are preserved" in {
    val gatlingUri = "https://user@gatling-tool.org:8080/?q=v"
    val extractedUris = extractUris(Seq(gatlingUri))

    extractedUris.vals shouldBe List(Value("uri1", "gatling-tool.org"))
    extractedUris.renderUri(gatlingUri).toString shouldBe "\"https://user@\" + uri1 + \"\"\":8080/?q=v\"\"\""
  }

  it should "same roots but different ports" in {
    val uri1 = "http://gatling-tool.org/path1/file"
    val uri2 = "http://gatling-tool.org:8080/path1/file"
    val extractedUris = extractUris(Seq(uri1, uri2))

    extractedUris.vals shouldBe List(Value("uri1", "gatling-tool.org"))

    extractedUris.renderUri(uri1).toString shouldBe "\"http://\" + uri1 + \"\"\"/path1/file\"\"\""
    extractedUris.renderUri(uri2).toString shouldBe "\"http://\" + uri1 + \"\"\":8080/path1/file\"\"\""
  }
}
