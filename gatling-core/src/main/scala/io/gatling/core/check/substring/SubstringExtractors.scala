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

package io.gatling.core.check.substring

import scala.annotation.tailrec

import io.gatling.commons.validation._
import io.gatling.core.check._

object SubstringExtractors {
  private def extractAll(prepared: String, criterion: String): List[Int] = {
    @tailrec
    def loop(fromIndex: Int, is: List[Int]): List[Int] =
      if (fromIndex >= prepared.length)
        is
      else
        prepared.indexOf(criterion, fromIndex) match {
          case -1 => is
          case i  => loop(i + criterion.length, i :: is)
        }

    loop(0, Nil)
  }

  def find(pattern: String, occurrence: Int): FindCriterionExtractor[String, String, Int] =
    new FindCriterionExtractor[String, String, Int](
      "substring",
      pattern,
      occurrence,
      text => {
        @tailrec
        def loop(fromIndex: Int, occ: Int): Validation[Option[Int]] =
          if (fromIndex >= pattern.length)
            Validation.NoneSuccess
          else
            text.indexOf(pattern, fromIndex) match {
              case -1 => Validation.NoneSuccess
              case i =>
                if (occ == occurrence)
                  Some(i).success
                else
                  loop(i + pattern.length, occ + 1)
            }

        loop(0, 0)
      }
    )

  def findAll(pattern: String): FindAllCriterionExtractor[String, String, Int] =
    new FindAllCriterionExtractor[String, String, Int](
      "substring",
      pattern,
      extractAll(_, pattern) match {
        case Nil => Validation.NoneSuccess
        case is  => Some(is.reverse).success
      }
    )

  def count(pattern: String): CountCriterionExtractor[String, String] =
    new CountCriterionExtractor[String, String](
      "substring",
      pattern,
      text => Some(extractAll(text, pattern).size).success
    )
}
