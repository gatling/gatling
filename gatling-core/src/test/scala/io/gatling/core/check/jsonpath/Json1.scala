/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.core.check.jsonpath

object Json1 extends JsonSample {

  val value = """{
                |	"store": {
                |		"book": [
                |			{	"category": "reference",
                |				"author": "Nigel Rees",
                |				"title": "Sayings of the Century",
                |				"display-price": 8.95
                |			},
                |			{	"category": "fiction",
                |				"author": "Evelyn Waugh",
                |				"title": "Sword of Honour",
                |				"display-price": 12.99
                |			},
                |			{	"category": "fiction",
                |				"author": "Herman Melville",
                |				"title": "Moby Dick",
                |				"isbn": "0-553-21311-3",
                |				"display-price": 8.99
                |		  	},
                |		  	{	"category": "fiction",
                |				"author": "J. R. R. Tolkien",
                |				"title": "The Lord of the Rings",
                |				"isbn": "0-395-19395-8",
                |				"display-price": 22.99
                |			}
                |		],
                |		"bicycle": {
                |			"foo": "baz",
                |			"color": "red",
                |			"display-price": 19.95,
                |			"foo:bar": "fooBar",
                |			"dot.notation": "new",
                |			"dash-notation": "dashes"
                |		}
                |	},
                |	"foo": "bar",
                |	"@id": "ID"
                |}""".stripMargin
}
