package io.gatling.core.check.extractor.jsonpath

import scala.collection.JavaConversions.{ asScalaBuffer, mapAsScalaConcurrentMap, mapAsScalaMap }
import scala.collection.concurrent

import org.jboss.netty.util.internal.ConcurrentHashMap

import io.gatling.core.check.Extractor
import io.gatling.core.check.extractor.Extractors.LiftedSeqOption
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.jsonpath.jsonsmart.JsonPath
import net.minidev.json.parser.JSONParser

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
		val filter: PartialFunction[Any, Map[String, Any]] = { case e: java.util.Map[_, _] => e.map { case (key, value) => key.toString -> value }.toMap }
	}

	implicit val anyJsonFilter = new JsonFilter[Any] {
		val filter: PartialFunction[Any, Any] = { case e => e }
	}
}

object JsonFilter extends LowPriorityJsonFilterImplicits

trait JsonFilter[X] {
	def filter: PartialFunction[Any, X]
}

object JsonPathExtractors {

	abstract class JsonPathExtractor[X] extends Extractor[Any, String, X] {
		val name = "jsonPath"
	}

	val cache: concurrent.Map[String, Validation[JsonPath]] = new ConcurrentHashMap[String, Validation[JsonPath]]
	def cached(expression: String): Validation[JsonPath] =
		if (configuration.core.extract.jsonPath.cache) cache.getOrElseUpdate(expression, compile(expression))
		else compile(expression)

	def parse(string: String) = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(string)

	def compile(expression: String) = JsonPath.compile(expression) match {
		case Left(error) => error.reason.failure
		case Right(path) => path.success
	}

	private def extractAll[X](json: Any, expression: String)(implicit filter: JsonFilter[X]): Validation[Iterator[X]] = {

		cached(expression).map { path =>
			path.query(json).collect(filter.filter)
		}
	}

	def extractOne[X](occurrence: Int)(implicit filter: JsonFilter[X]) = new JsonPathExtractor[X] {

		def apply(prepared: Any, criterion: String): Validation[Option[X]] =
			extractAll(prepared, criterion).map(_.toStream.liftSeqOption.flatMap(_.lift(occurrence)))
	}

	def extractMultiple[X](implicit filter: JsonFilter[X]) = new JsonPathExtractor[Seq[X]] {

		def apply(prepared: Any, criterion: String): Validation[Option[Seq[X]]] =
			extractAll(prepared, criterion).map(_.toVector.liftSeqOption.flatMap(_.liftSeqOption))
	}

	def count = new JsonPathExtractor[Int] {

		def apply(prepared: Any, criterion: String): Validation[Option[Int]] =
			extractAll[Any](prepared, criterion).map(i => Some(i.size))
	}
}
