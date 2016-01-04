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
package io.gatling.recorder.config

import java.io.{ FileNotFoundException, File }
import java.nio.file.Paths

import io.gatling.BaseSpec
import io.gatling.commons.util.PathHelper._

import org.scalatest.BeforeAndAfter

class RecorderConfigurationSpec extends BaseSpec with BeforeAndAfter {

  val NON_EXISTING_DIR = "nonExistingDir"
  val EXISTING_DIR = "existingDir"
  val FILE_NAME = "file"

  def removeAll(): Unit = {
    new File(EXISTING_DIR, FILE_NAME).delete()
    new File(EXISTING_DIR).delete()
    new File(FILE_NAME).delete()
  }

  def file2path(file: File) = Paths.get(file.toURI)

  before(removeAll())
  after(removeAll())

  "Recorder Configuration" should "create file if directory exists" in {
    val resFile = RecorderConfiguration.createAndOpen(file2path(new File(FILE_NAME)))
    resFile.exists shouldBe true
  }

  it should "create if parent directory exists" in {
    new File(EXISTING_DIR).mkdir()
    val testFile = new File(EXISTING_DIR, FILE_NAME)
    val resFile = RecorderConfiguration.createAndOpen(file2path(testFile))
    resFile.exists shouldBe true
  }

  it should "throw exception if parent directory is missing" in {
    val testFile = new File(NON_EXISTING_DIR, FILE_NAME)
    a[FileNotFoundException] should be thrownBy RecorderConfiguration.createAndOpen(file2path(testFile))
  }
}
