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
package io.gatling.core.check.extractor.substring

import scala.annotation.tailrec

import io.gatling.commons.validation._
import io.gatling.core.check.extractor.{ CountExtractor, MultipleExtractor, SingleExtractor, CriterionExtractorFactory }

object SubstringExtractorFactory extends CriterionExtractorFactory[String, String]("substring") {

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

  implicit def defaultSingleExtractor = new SingleExtractor[String, String, Int] {

    def extract(prepared: String, criterion: String, occurrence: Int): Validation[Option[Int]] = {

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

  implicit def defaultMultipleExtractor = new MultipleExtractor[String, String, Int] {
    def extract(prepared: String, criterion: String): Validation[Option[Seq[Int]]] =
      extractAll(prepared, criterion) match {
        case Nil => NoneSuccess
        case is  => Some(is.reverse).success
      }
  }

  implicit val defaultCountExtractor = new CountExtractor[String, String] {
    def extract(prepared: String, criterion: String): Validation[Option[Int]] =
      Some(extractAll(prepared, criterion).size).success
  }
}
