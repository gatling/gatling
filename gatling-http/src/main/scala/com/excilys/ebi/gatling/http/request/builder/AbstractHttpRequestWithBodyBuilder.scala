package com.excilys.ebi.gatling.http.request.builder

import org.fusesource.scalate._

import com.ning.http.client.RequestBuilder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.context.FromContext
import com.excilys.ebi.gatling.core.util.PathHelper._
import com.excilys.ebi.gatling.core.util.FileHelper._

import com.excilys.ebi.gatling.http.action.builder.HttpRequestActionBuilder
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.TemplateBody
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam

import java.io.File

abstract class AbstractHttpRequestWithBodyBuilder[B <: AbstractHttpRequestWithBodyBuilder[B]](httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]],
	headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[Tuple2[String, String]])
		extends AbstractHttpRequestBuilder[B](httpRequestActionBuilder, urlFormatter, queryParams, headers, followsRedirects, credentials) {

	override def getRequestBuilder(context: Context): RequestBuilder = {
		val requestBuilder = super.getRequestBuilder(context)
		requestBuilder setMethod getMethod
		addBodyTo(requestBuilder, body, context)
		requestBuilder
	}

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], body: Option[HttpRequestBody], followsRedirects: Option[Boolean], credentials: Option[Tuple2[String, String]]): B

	def newInstance(httpRequestActionBuilder: HttpRequestActionBuilder, urlFormatter: Option[Context => String], queryParams: Option[Map[String, Param]], headers: Option[Map[String, String]], followsRedirects: Option[Boolean], credentials: Option[Tuple2[String, String]]): B = {
		newInstance(httpRequestActionBuilder, urlFormatter, queryParams, headers, body, followsRedirects, credentials)
	}

	def withFile(filePath: String): B = {
		newInstance(httpRequestActionBuilder, urlFormatter, queryParams, headers, Some(FilePathBody(filePath)), followsRedirects, credentials)
	}

	def withBody(body: String): B = {
		newInstance(httpRequestActionBuilder, urlFormatter, queryParams, headers, Some(StringBody(body)), followsRedirects, credentials)
	}

	def withTemplateBody(tplPath: String, values: Map[String, Any]): B = {
		val encapsulatedValues: Map[String, Param] = values.map {
			value =>
				(value._1, value._2 match {
					case FromContext(s) => ContextParam(s)
					case s => StringParam(s.toString)
				})
		}
		newInstance(httpRequestActionBuilder, urlFormatter, queryParams, headers, Some(TemplateBody(tplPath, encapsulatedValues)), followsRedirects, credentials)
	}

	def addBodyTo(requestBuilder: RequestBuilder, body: Option[HttpRequestBody], context: Context) = {
		body match {
			case Some(thing) =>
				thing match {
					case FilePathBody(filePath) => requestBuilder setBody new File(GATLING_REQUEST_BODIES_FOLDER + "/" + filePath)
					case StringBody(body) => requestBuilder setBody body
					case TemplateBody(tplPath, values) => requestBuilder setBody compileBody(tplPath, values, context)
					case _ =>
				}
			case None =>
		}
	}

	def compileBody(tplPath: String, values: Map[String, Param], context: Context): String = {

		val engine = new TemplateEngine
		engine.allowCaching = false

		var bindings: List[Binding] = List()
		var templateValues: Map[String, String] = Map.empty

		for (value <- values) {
			bindings = Binding(value._1, "String") :: bindings
			templateValues = templateValues + (value._1 -> (value._2 match {
				case StringParam(string) => string
				case ContextParam(string) => context.getAttribute(string).toString
			}))
		}

		engine.bindings = bindings
		engine.layout(GATLING_TEMPLATES_FOLDER + "/" + tplPath + SSP_EXTENSION, templateValues)
	}
}