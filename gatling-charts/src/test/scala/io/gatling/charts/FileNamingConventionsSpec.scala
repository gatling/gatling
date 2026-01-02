/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.charts

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers

class FileNamingConventionsSpec extends AnyFlatSpecLike with Matchers {
  "toRequestFileName" should "generate non clashing names" in {
    val name1 = "Delete / Delete:16d8a886-add8-49ec-a481-1b41579256e9"
    val name2 = "Delete / Delete:bdfd117c-ee42-4572-b07c-a7e79da131ef"
    name1.toRequestFileName shouldNot be(name2.toRequestFileName)
  }
}
