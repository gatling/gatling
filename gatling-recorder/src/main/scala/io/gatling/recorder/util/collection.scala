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
package io.gatling.recorder.util

private[recorder] object collection {

  implicit class RichSeq[T](val elts: Seq[T]) extends AnyVal {

    // See ScenarioSpec for example
    def groupAsLongAs(p: T => Boolean): List[List[T]] =
      elts.foldRight(List[List[T]]()) {
        case (t, Nil) => (t :: Nil) :: Nil
        case (t, xs @ xh :: xt) =>
          if (p(t)) (t :: xh) :: xt
          else (t :: Nil) :: xs
      }

    def splitWhen(p: T => Boolean): List[List[T]] =
      elts.foldLeft(List.empty[List[T]])({
        case (Nil, x)          => List(x) :: Nil
        case (l @ (h :: t), x) => if (p(x)) List(x) :: l else (x :: h) :: t
      }).map(_.reverse).reverse
  }
}
