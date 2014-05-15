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

import java.io.{ File => JFile }
import java.net.{ URISyntaxException, URL }

import scala.util.Try

import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }

/**
 * This object groups all utilities for files
 */
object FileHelper {

  implicit class RichURL(val url: URL) extends AnyVal {

    def jfile: JFile = Try(new JFile(url.toURI))
      .recover { case e: URISyntaxException => new JFile(url.getPath) }
      .get
  }

  implicit class RichFile(val file: JFile) extends AnyVal {

    def validateExistingReadable(): Validation[JFile] =
      if (!file.exists)
        s"File $file doesn't exist".failure
      else if (!file.canRead)
        s"File $file can't be read".failure
      else
        file.success
  }
}
