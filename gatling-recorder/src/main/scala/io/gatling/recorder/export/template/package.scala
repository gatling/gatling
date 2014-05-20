package io.gatling.recorder.export

import com.dongxiguo.fastring.Fastring.Implicits._

package object template {

  private val tripleQuotes = '"'.toString * 3
  def protectWithTripleQuotes(string: String): Fastring = fast"$tripleQuotes$string$tripleQuotes"
}