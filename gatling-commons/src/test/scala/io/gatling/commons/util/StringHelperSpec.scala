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
package io.gatling.commons.util

import java.security.MessageDigest

import io.gatling.BaseSpec
import io.gatling.commons.util.Io._
import io.gatling.commons.util.StringHelper.RichString

class StringHelperSpec extends BaseSpec {

  val fileBytes = getClass.getResource("/emoticon.png").toByteArray

  "bytes2Hex" should "correctly compute file sha-1" in {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(fileBytes)
    val digestBytes = md.digest
    StringHelper.bytes2Hex(digestBytes) shouldBe "665a5bf97191eb3d8b2a20d833182313343af073"
  }

  it should "correctly compute file md5" in {
    val md = MessageDigest.getInstance("MD5")
    md.update(fileBytes)
    val digestBytes = md.digest
    StringHelper.bytes2Hex(digestBytes) shouldBe "08f6575e7712febe2f529e1ea2c0179e"
  }

  "truncate" should "truncate the string when its length exceeds the max length" in {
    "hello".truncate(2) shouldBe "he..."
  }

  it should "left the string untouched when the string does not exceeds the max length" in {
    "hello".truncate(6) shouldBe "hello"
  }

  "leftPad" should "pad correctly a two digits number" in {
    "12".leftPad(6) shouldBe "    12"
  }

  it should "not pad when the number of digits is higher than the expected string size" in {
    "123456".leftPad(4) shouldBe "123456"
  }

  "rightPad" should "pad correctly a two digits number" in {
    "12".rightPad(6) shouldBe "12    "
  }

  it should "not pad when the number of digits is higher than the expected string size" in {
    "123456".rightPad(4) shouldBe "123456"
  }

  "unsafeChars" should "correctly get the char array corresponding to the string" in {
    "foo".unsafeChars shouldBe Array('f', 'o', 'o')
  }

  "RichCharSequence.indexOf" should "find target when placed at the beginning" in {
    StringHelper.RichCharSequence("${foobar}").indexOf("${", 0) shouldBe 0
  }

  it should "not find target when placed at the beginning but there's an offset" in {
    StringHelper.RichCharSequence("${foobar}").indexOf("${", 1) shouldBe -1
  }

  it should "find target when placed at the middle" in {
    StringHelper.RichCharSequence("foo${bar}").indexOf("${", 0) shouldBe 3
  }

  it should "find target when placed at the middle and there's an inferior offset" in {
    StringHelper.RichCharSequence("foo${bar}").indexOf("${", 2) shouldBe 3
  }

  it should "not find target when placed at the middle and there's an superior offset" in {
    StringHelper.RichCharSequence("foo${bar}").indexOf("${", 4) shouldBe -1
  }

  it should "not find target when target is longer" in {
    StringHelper.RichCharSequence("$").indexOf("${", 0) shouldBe -1
  }

  "RichCharSequence.contains" should "find an existing char in a sequence" in {
    StringHelper.RichCharSequence("foo bar").contains(_ == ' ') shouldBe true
  }

  it should "not find an missing char in a sequence" in {
    StringHelper.RichCharSequence("foo bar").contains(_ == 'z') shouldBe false
  }
}
