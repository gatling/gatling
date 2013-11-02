/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.http

import io.gatling.core.filter.{ FilterList, FilterListWrapper }
import io.gatling.http.dom.EmbeddedResource

package object dom {

	implicit class EmbeddedResourcesFilter(val filters: List[FilterList]) extends AnyVal {

		def filter(resources: List[EmbeddedResource]) = resources.filter(res => filters.accept(res.uri.toString))
	}
}
