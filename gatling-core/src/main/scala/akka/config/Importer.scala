/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package akka.config

import java.io.{ BufferedReader, File, FileInputStream, InputStream, InputStreamReader }

/**
 * An interface for finding config files and reading them into strings for
 * parsing. This is used to handle `include` directives in config files.
 */
trait Importer {

  def importFile(filename: String): String

  private val BUFFER_SIZE = 8192

  protected def streamToString(in: InputStream): String = {
    try {
      val reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))
      val buffer = new Array[Char](BUFFER_SIZE)
      val sb = new StringBuilder
      var n = 0
      while (n >= 0) {
        n = reader.read(buffer, 0, buffer.length)
        if (n >= 0) {
          sb.appendAll(buffer, 0, n)
        }
      }
      in.close()
      sb.toString
    } catch {
      case x => throw new ConfigurationException(x.toString)
    }
  }
}

/**
 * An Importer that looks for imported config files in the filesystem.
 * This is the default importer.
 */
class FilesystemImporter(val baseDir: String) extends Importer {
  def importFile(filename: String): String = {
    val f = new File(filename)
    val file = if (f.isAbsolute) f else new File(baseDir, filename)
    streamToString(new FileInputStream(file))
  }
}

/**
 * An Importer that looks for imported config files in the java resources
 * of the system class loader (usually the jar used to launch this app).
 */
class ResourceImporter(classLoader: ClassLoader) extends Importer {
  def importFile(filename: String): String = {
    val stream = classLoader.getResourceAsStream(filename)
    streamToString(stream)
  }
}
