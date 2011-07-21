package com.excilys.ebi.gatling.http.request.builder.mixin

import org.fusesource.scalate._

import com.ning.http.client.RequestBuilder

import com.excilys.ebi.gatling.core.context.Context

import com.excilys.ebi.gatling.http.request.builder.HttpRequestBuilder
import com.excilys.ebi.gatling.http.request.HttpRequestBody
import com.excilys.ebi.gatling.http.request.FilePathBody
import com.excilys.ebi.gatling.http.request.StringBody
import com.excilys.ebi.gatling.http.request.TemplateBody

import java.io.File

trait Body extends HttpRequestBuilder {

  abstract override def build(context: Context, method: String) = {
    requestBuilder setMethod method
    addBodyTo(requestBuilder, body)
    super.build(context, method)
  }

  def withFile(filePath: String): HttpRequestBuilder

  def withBody(body: String): HttpRequestBuilder

  def withTemplateBody(tplPath: String, values: Map[String, String]): HttpRequestBuilder

  def addBodyTo(requestBuilder: RequestBuilder, body: Option[HttpRequestBody]) = {
    body match {
      case Some(thing) =>
        thing match {
          case FilePathBody(filePath) => requestBuilder setBody new File("user-requests/" + filePath)
          case StringBody(body) => requestBuilder setBody body
          case TemplateBody(tplPath, values) => requestBuilder setBody compileBody(tplPath, values)
          case _ =>
        }
      case None =>
    }
  }

  def compileBody(tplPath: String, values: Map[String, String]): String = {

    val engine = new TemplateEngine
    engine.allowCaching = false

    var bindings: List[Binding] = List()

    for (value <- values) {
      bindings = Binding(value._1, "String") :: bindings
    }

    engine.bindings = bindings
    engine.layout("user-templates/" + tplPath + ".ssp", values)
  }
}