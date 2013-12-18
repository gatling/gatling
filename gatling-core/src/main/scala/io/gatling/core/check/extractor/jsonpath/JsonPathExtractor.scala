package io.gatling.core.check.extractor.jsonpath

import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.concurrent

import io.gatling.core.check.extractor.{ CriterionExtractor, LiftedSeqOption }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.jsonpath.JsonPath
import jsr166e.ConcurrentHashMapV8
import net.minidev.json.parser.JSONParser

object JsonPathExtractor {

	val cache: concurrent.Map[String, Validation[JsonPath]] = new ConcurrentHashMapV8[String, Validation[JsonPath]]

	def cached(expression: String): Validation[JsonPath] =
		if (configuration.core.extract.jsonPath.cache) cache.getOrElseUpdate(expression, compile(expression))
		else compile(expression)

	def parse(bytes: Array[Byte]) = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(bytes)
	def parse(string: String) = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(string)

	def compile(expression: String): Validation[JsonPath] = JsonPath.compile(expression) match {
		case Left(error) => error.reason.failure
		case Right(path) => path.success
	}

	def extractAll[X: JsonFilter](json: Any, expression: String): Validation[Iterator[X]] =
		cached(expression).map(_.query(json).collect(implicitly[JsonFilter[X]].filter))
}

abstract class JsonPathExtractor[X] extends CriterionExtractor[Any, String, X] { val criterionName = "jsonPath" }

class SingleJsonPathExtractor[X: JsonFilter](val criterion: String, occurrence: Int) extends JsonPathExtractor[X] {

	def extract(prepared: Any): Validation[Option[X]] =
		JsonPathExtractor.extractAll(prepared, criterion).map(_.toStream.liftSeqOption.flatMap(_.lift(occurrence)))
}

class MultipleJsonPathExtractor[X: JsonFilter](val criterion: String) extends JsonPathExtractor[Seq[X]] {

	def extract(prepared: Any): Validation[Option[Seq[X]]] =
		JsonPathExtractor.extractAll(prepared, criterion).map(_.toVector.liftSeqOption.flatMap(_.liftSeqOption))
}

class CountJsonPathExtractor(val criterion: String) extends JsonPathExtractor[Int] {

	def extract(prepared: Any): Validation[Option[Int]] =
		JsonPathExtractor.extractAll[Any](prepared, criterion).map(i => Some(i.size))
}
