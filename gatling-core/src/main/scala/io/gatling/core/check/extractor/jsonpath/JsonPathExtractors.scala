package io.gatling.core.check.extractor.jsonpath

import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.concurrent

import org.jboss.netty.util.internal.ConcurrentHashMap

import io.gatling.core.check.CriterionExtractor
import io.gatling.core.check.extractor.Extractors.LiftedSeqOption
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.session.Expression
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.jsonpath.jsonsmart.JsonPath
import net.minidev.json.parser.JSONParser

object JsonPathExtractor {

	val cache: concurrent.Map[String, Validation[JsonPath]] = new ConcurrentHashMap[String, Validation[JsonPath]]
	def cached(expression: String): Validation[JsonPath] =
		if (configuration.core.extract.jsonPath.cache) cache.getOrElseUpdate(expression, compile(expression))
		else compile(expression)

	def parse(string: String) = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE).parse(string)

	def compile(expression: String) = JsonPath.compile(expression) match {
		case Left(error) => error.reason.failure
		case Right(path) => path.success
	}

	def extractAll[X](json: Any, expression: String)(implicit filter: JsonFilter[X]): Validation[Iterator[X]] =
		cached(expression).map(_.query(json).collect(filter.filter))
}

abstract class JsonPathExtractor[X] extends CriterionExtractor[Any, String, X] {
	val name = "jsonPath"
}

class OneJsonPathExtractor[X](val criterion: Expression[String], occurrence: Int)(implicit filter: JsonFilter[X]) extends JsonPathExtractor[X] {

	def extract(prepared: Any, criterion: String): Validation[Option[X]] =
		JsonPathExtractor.extractAll(prepared, criterion).map(_.toStream.liftSeqOption.flatMap(_.lift(occurrence)))
}

class MultipleJsonPathExtractor[X](val criterion: Expression[String])(implicit filter: JsonFilter[X]) extends JsonPathExtractor[Seq[X]] {

	def extract(prepared: Any, criterion: String): Validation[Option[Seq[X]]] =
		JsonPathExtractor.extractAll(prepared, criterion).map(_.toVector.liftSeqOption.flatMap(_.liftSeqOption))
}

class CountJsonPathExtractor(val criterion: Expression[String]) extends JsonPathExtractor[Int] {

	def extract(prepared: Any, criterion: String): Validation[Option[Int]] =
		JsonPathExtractor.extractAll[Any](prepared, criterion).map(i => Some(i.size))
}
