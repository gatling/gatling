/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling

import io.gatling.commons.util.StringHelper._

package object charts {
  private[charts] implicit class FileNamingConventions(val string: String) extends AnyVal {

    /**
     * Transform a string to a simpler one that can be used safely as file name
     *
     * @return
     *   a simplified string
     */
    private def toFileName = {
      val trimmed = string.trim match {
        case "" => "missing_name"
        case s  => s
      }

      trimmed.clean.take(15) + "-" + trimmed.hashCode
    }

    def toRequestFileName = s"req_$toFileName"

    def toGroupFileName = s"group_$toFileName"
  }
}
