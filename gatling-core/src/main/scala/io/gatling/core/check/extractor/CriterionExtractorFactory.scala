package io.gatling.core.check.extractor

import io.gatling.core.validation.Validation

abstract class SingleExtractor[P, T, X] {
  def extract(prepared: P, criterion: T, occurrence: Int): Validation[Option[X]]
}

abstract class MultipleExtractor[P, T, X] {
  def extract(prepared: P, criterion: T): Validation[Option[Seq[X]]]
}

abstract class CountExtractor[P, T] {
  def extract(prepared: P, criterion: T): Validation[Option[Int]]
}

abstract class CriterionExtractorFactory[P, T](name: String) {

  def newSingleExtractor[X](_criterion: T, _occurrence: Int)(implicit extractor: SingleExtractor[P, T, X]) =
    new CriterionExtractor[P, T, X] with FindArity {

      override def criterionName = CriterionExtractorFactory.this.name

      override def criterion = _criterion

      override def occurrence = _occurrence

      override def extract(prepared: P): Validation[Option[X]] = extractor.extract(prepared, _criterion, _occurrence)
    }

  def newMultipleExtractor[X](_criterion: T)(implicit extractor: MultipleExtractor[P, T, X]) =
    new CriterionExtractor[P, T, Seq[X]] with FindAllArity {

      override def criterionName = CriterionExtractorFactory.this.name

      override def criterion = _criterion

      override def extract(prepared: P): Validation[Option[Seq[X]]] = extractor.extract(prepared, _criterion)
    }

  def newCountExtractor(_criterion: T)(implicit extractor: CountExtractor[P, T]) =
    new CriterionExtractor[P, T, Int] with CountArity {

      override def criterionName = CriterionExtractorFactory.this.name

      override def criterion = _criterion

      override def extract(prepared: P): Validation[Option[Int]] = extractor.extract(prepared, _criterion)
    }
}
