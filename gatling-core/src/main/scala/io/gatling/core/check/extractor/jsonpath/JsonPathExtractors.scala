package io.gatling.core.check.extractor.jsonpath

import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.concurrent

import org.jboss.netty.util.internal.ConcurrentHashMap

import io.gatling.core.check.Extractor
import io.gatling.core.check.extractor.Extractors.LiftedSeqOption
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.validation.{ FailureWrapper, SuccessWrapper, Validation }
import io.gatling.jsonpath.jsonsmart.JsonPath
import net.minidev.json.parser.JSONParser

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

	private def extractAll(json: Any, expression: String): Validation[Iterator[String]] = {

		cached(expression).map { path =>
			path.query(json).map(_.toString)
		}
	}

	val extractOne = (occurrence: Int) => new JsonPathExtractor[String] {

		def apply(prepared: Any, criterion: String): Validation[Option[String]] =
			extractAll(prepared, criterion).map(_.toStream.liftSeqOption.flatMap(_.lift(occurrence)))
	}

	val extractMultiple = new JsonPathExtractor[Seq[String]] {

		def apply(prepared: Any, criterion: String): Validation[Option[Seq[String]]] =
			extractAll(prepared, criterion).map(_.toVector.liftSeqOption.flatMap(_.liftSeqOption))
	}

	val count = new JsonPathExtractor[Int] {

		def apply(prepared: Any, criterion: String): Validation[Option[Int]] =
			extractAll(prepared, criterion).map(i => Some(i.size))
	}
}
