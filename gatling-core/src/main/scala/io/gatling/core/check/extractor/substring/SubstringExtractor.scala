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
package io.gatling.core.check.extractor.substring

import io.gatling.core.check.extractor.{ CountArity, FindAllArity, FindArity, CriterionExtractor }
import io.gatling.core.validation._

import scala.annotation.tailrec

abstract class SubstringExtractorBase[X] extends CriterionExtractor[String, String, X] {

  val criterionName = "substring"
}

class SingleSubstringExtractor(val criterion: String, val occurrence: Int) extends SubstringExtractorBase[Int] with FindArity {

  def extract(prepared: String): Validation[Option[Int]] = {

      @tailrec
      def loop(fromIndex: Int, occ: Int): Validation[Option[Int]] =
        if (fromIndex >= prepared.length)
          NoneSuccess
        else
          prepared.indexOf(criterion, fromIndex) match {
            case -1 => NoneSuccess
            case i =>
              if (occ == occurrence)
                Some(i).success
              else
                loop(i + criterion.length, occ + 1)
          }

    loop(0, 0)
  }
}

class MultipleSubstringExtractor(val criterion: String) extends SubstringExtractorBase[Seq[Int]] with FindAllArity {

  def extract(prepared: String): Validation[Option[Seq[Int]]] = {

      @tailrec
      def loop(fromIndex: Int, is: List[Int]): List[Int] =
        if (fromIndex >= prepared.length)
          is
        else
          prepared.indexOf(criterion, fromIndex) match {
            case -1 => is
            case i  => loop(i + criterion.length, i :: is)
          }

    loop(0, Nil) match {
      case Nil => NoneSuccess
      case is  => Some(is.reverse).success
    }
  }
}

class CountSubstringExtractor(val criterion: String) extends SubstringExtractorBase[Int] with CountArity {

  def extract(prepared: String): Validation[Option[Int]] = {

      @tailrec
      def loop(fromIndex: Int, count: Int): Int =
        if (fromIndex >= prepared.length)
          count
        else
          prepared.indexOf(criterion, fromIndex) match {
            case -1 => count
            case i  => loop(i + criterion.length, count + 1)
          }

    val count = loop(0, 0)
    Some(count).success
  }
}
