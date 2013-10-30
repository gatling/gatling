package io.gatling.core.check.extractor.jsonpath

import scala.annotation.implicitNotFound
import scala.collection.JavaConversions.{ asScalaBuffer, mapAsScalaMap }
import scala.collection.breakOut

trait LowPriorityJsonFilterImplicits {

	implicit val stringJsonFilter = new JsonFilter[String] {
		val filter: PartialFunction[Any, String] = { case e: Any => e.toString }
	}

	implicit val integerJsonFilter = new JsonFilter[Int] {
		val filter: PartialFunction[Any, Int] = { case e: Integer => e }
	}

	implicit val jLongJsonFilter = new JsonFilter[Long] {
		val filter: PartialFunction[Any, Long] = { case e: java.lang.Long => e }
	}

	implicit val jDoubleJsonFilter = new JsonFilter[Double] {
		val filter: PartialFunction[Any, Double] = { case e: java.lang.Double => e }
	}

	implicit val jFloatJsonFilter = new JsonFilter[Float] {
		val filter: PartialFunction[Any, Float] = { case e: java.lang.Float => e }
	}

	implicit val jListJsonFilter = new JsonFilter[Seq[Any]] {
		val filter: PartialFunction[Any, Seq[Any]] = { case e: java.util.List[_] => e }
	}

	implicit val jMapJsonFilter = new JsonFilter[Map[String, Any]] {
		val filter: PartialFunction[Any, Map[String, Any]] = { case e: java.util.Map[_, _] => e.map { case (key, value) => key.toString -> value }(breakOut) }
	}

	implicit val anyJsonFilter = new JsonFilter[Any] {
		val filter: PartialFunction[Any, Any] = { case e => e }
	}
}

object JsonFilter extends LowPriorityJsonFilterImplicits

@implicitNotFound(msg = "Cannot find JsonFilter type class for type ${X}")
trait JsonFilter[X] {
	def filter: PartialFunction[Any, X]
}