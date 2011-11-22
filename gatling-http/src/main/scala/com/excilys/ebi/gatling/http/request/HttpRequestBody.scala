/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.http.request
import com.excilys.ebi.gatling.core.context.Context

/**
 * Class used for polymorphism only
 */
abstract class HttpRequestBody

/**
 * Wraps a body represented by a string
 *
 * @param string the string representing the body
 */
case class StringBody(string: String) extends HttpRequestBody
/**
 * Wraps a body that is in a file
 *
 * @param filePath the path to the file containing the body
 */
case class FilePathBody(filePath: String) extends HttpRequestBody
/**
 * Wraps a body that requires template compilation
 *
 * @param tplPath the path to the template
 * @param values the values that will be merged in the template
 */
case class TemplateBody(tplPath: String, values: Map[String, Context => String]) extends HttpRequestBody
