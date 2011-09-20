package com.excilys.ebi.gatling.http.request.builder.mixin

import org.fusesource.scalate._

import com.ning.http.client.RequestBuilder

import com.excilys.ebi.gatling.core.context.Context
import com.excilys.ebi.gatling.core.util.PathHelper._
import com.excilys.ebi.gatling.core.util.FileHelper._

import com.excilys.ebi.gatling.http.request.builder.HttpRequestBuilder
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.TemplateBody
import com.excilys.ebi.gatling.http.request.Param
import com.excilys.ebi.gatling.http.request.StringParam
import com.excilys.ebi.gatling.http.request.ContextParam

import java.io.File

trait Body extends HttpRequestBuilder {

  abstract override def build(context: Context, method: String) = {
    requestBuilder setMethod method
    addBodyTo(requestBuilder, body, context)
    super.build(context, method)
  }

  def withFile(filePath: String): HttpRequestBuilder

  def withBody(body: String): HttpRequestBuilder

  def withTemplateBodyFromContext(tplPath: String, values: Map[String, String]): HttpRequestBuilder

  def withTemplateBody(tplPath: String, values: Map[String, String]): HttpRequestBuilder

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
        case ContextParam(string) => context.getAttribute(string)
      }))
    }

    engine.bindings = bindings
    engine.layout(GATLING_TEMPLATES_FOLDER + "/" + tplPath + SSP_EXTENSION, templateValues)
  }
}