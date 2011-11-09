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

/**
 * This trait represent a request parameter entered by the user
 */
trait Param
/**
 * This parameter is a string, it will be used as is
 *
 * @param string the value of the parameter
 */
case class StringParam(string: String) extends Param
/**
 * This parameter is to be extracted from the context of the scenario
 *
 * @param string the context key in which the value should be extracted
 */
case class ContextParam(string: String) extends Param