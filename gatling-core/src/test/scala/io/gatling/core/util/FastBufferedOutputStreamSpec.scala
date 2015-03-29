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
package io.gatling.core.util

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8

import io.gatling.BaseSpec

class FastBufferedOutputStreamSpec extends BaseSpec {

  def toString(baos: ByteArrayOutputStream) = baos.toString(UTF_8.name())
  def toByteArray(s: String) = s.getBytes(UTF_8)

  "FastBufferedOutputStream" should "write directly to the underlying OutputStream if bufferSize = 0" in {
    val baos = new ByteArrayOutputStream()
    val fbos = new FastBufferedOutputStream(baos, 0)
    fbos.write(toByteArray("foo"))
    toString(baos) shouldBe "foo"
  }

  it should "not write to the underlying OutputStream until there was enough data to force a flush" in {
    val baos = new ByteArrayOutputStream()
    val fbos = new FastBufferedOutputStream(baos, 6)
    fbos.write(toByteArray("foo"))
    toString(baos) shouldBe ""
    fbos.write(toByteArray("bar"))
    toString(baos) shouldBe ""
    fbos.write(toByteArray("quz"))
    toString(baos) shouldBe "foobar"
  }

  it should "be forced to write to the underlying OutputStream when calling flush()" in {
    val baos = new ByteArrayOutputStream()
    val fbos = new FastBufferedOutputStream(baos, 6)
    fbos.write(toByteArray("foo"))
    toString(baos) shouldBe ""
    fbos.flush()
    toString(baos) shouldBe "foo"
  }

}
