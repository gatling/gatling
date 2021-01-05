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

import com.typesafe.config.Config

object ConfigHelper {

  /**
   * Creates a configuration with a series of fallback configurations
   * in which config keys will be looked up.
   * @param config the root configuration
   * @param fallbacks the list of fallback configurations, ordered by their precedence in
   *                  the fallback chain.
   * @return the configuration with its fallback configs configured
   */
  def configChain(config: Config, fallbacks: Config*): Config =
    fallbacks.foldLeft(config)(_ withFallback _).resolve

  implicit class PimpedConfig(val config: Config) extends AnyVal {

    def withChild[T](path: String)(f: Config => T): T = f(config.getConfig(path))

    def getStringOption(path: String): Option[String] =
      if (config.hasPath(path)) Some(config.getString(path)) else None

    def getIntOption(path: String): Option[Int] =
      if (config.hasPath(path)) Some(config.getInt(path)) else None

    def getLongOption(path: String): Option[Long] =
      if (config.hasPath(path)) Some(config.getLong(path)) else None

    def getBooleanOption(path: String): Option[Boolean] =
      if (config.hasPath(path)) Some(config.getBoolean(path)) else None
  }
}
