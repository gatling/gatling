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

package io.gatling.core.stats.writer

object DataWriterType {

  private val AllTypes = Seq(Console, File, Graphite)
    .map(t => t.name -> t)
    .toMap

  def findByName(name: String): Option[DataWriterType] = AllTypes.get(name)

  object Console extends DataWriterType("console")
  object File extends DataWriterType("file")
  object Graphite extends DataWriterType("graphite")
}

sealed abstract class DataWriterType(val name: String)
