package io.gatling.recorder.util

object collection {

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
				case (Nil, x) => List(x) :: Nil
				case (l @ (h :: t), x) => if (p(x)) (x :: h) :: t else List(x) :: l
			}).map(_.reverse).reverse
	}
}
