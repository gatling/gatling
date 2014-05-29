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
package io.gatling.recorder.config

import java.io.{FileNotFoundException, File}

import org.junit.runner.RunWith

import org.specs2.mutable.{BeforeAfter, Specification}
import org.specs2.runner.JUnitRunner


@RunWith(classOf[JUnitRunner])
class RecorderConfigrationSpec extends Specification  {
  sequential

  val NON_EXISTING_DIR = "nonExistingDir"
  val EXISTING_DIR = "existingDir"
  val FILE_NAME = "file"

  class Context extends BeforeAfter {
    def removeAll() {
      new File(EXISTING_DIR, FILE_NAME).delete()
      new File(EXISTING_DIR).delete()
      new File(FILE_NAME).delete()
    }

    def before() {
      removeAll()
    }

    def after() {
      removeAll()
    }
  }

  "recorder configuration" should {
    "create file if directory exists" in new Context {
      val resFile = RecorderConfiguration.createAndOpen(new File(FILE_NAME))
      resFile.exists must beTrue
    }

    "create if parent directory exists" in new Context {
      new File(EXISTING_DIR).mkdir()
      val testFile = new File(EXISTING_DIR, FILE_NAME)
      val resFile = RecorderConfiguration.createAndOpen(testFile)
      resFile.exists must beTrue
    }

    "throw exception if parent directory is missing" in new Context {
      val testFile = new File(NON_EXISTING_DIR, FILE_NAME)
      RecorderConfiguration.createAndOpen(testFile) must throwA[FileNotFoundException]
    }
  }
}
