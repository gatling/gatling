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

package io.gatling.commons.util

import java.security.MessageDigest

import scala.util.Using

import io.gatling.BaseSpec
import io.gatling.commons.util.Io._

class HexSpec extends BaseSpec {

  private val fileBytes = Using.resource(getClass.getResourceAsStream("/emoticon.png"))(_.toByteArray())

  "toHexString" should "correctly compute file sha-1" in {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(fileBytes)
    val digestBytes = md.digest
    Hex.toHexString(digestBytes) shouldBe "665a5bf97191eb3d8b2a20d833182313343af073"
  }

  it should "correctly compute file md5" in {
    val md = MessageDigest.getInstance("MD5")
    md.update(fileBytes)
    val digestBytes = md.digest
    Hex.toHexString(digestBytes) shouldBe "08f6575e7712febe2f529e1ea2c0179e"
  }
}
