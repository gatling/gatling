/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.bundle

import io.gatling.plugin.io.{ PluginIO, PluginLogger, PluginScanner }

object BundleIO extends PluginIO {
  private val in = scala.io.Source.stdin.getLines()

  override def getLogger: PluginLogger = new PluginLogger {
    // We use println here as it is a CLI, where log.info is disabled by default
    override def info(message: String): Unit = println(message)
    override def error(message: String): Unit = Console.err.println(message)
  }

  override def getScanner: PluginScanner = new PluginScanner {
    override def readString(): String = in.next().trim

    override def readInt(): Int = in.next().trim.toInt
  }
}
