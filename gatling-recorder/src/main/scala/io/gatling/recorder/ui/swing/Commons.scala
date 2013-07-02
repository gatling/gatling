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
package io.gatling.recorder.ui.swing

import scala.swing.Swing.Icon

object Commons {
	val logoSmall = Icon(getClass.getResource("img/logo_small.png"))

	private val iconsFiles = List("img/fav_small.png", "img/fav_big.png", "img/picto_small.png", "img/picto_big.png")
	val iconList = iconsFiles.map(getClass.getResource).map(Icon(_).getImage)
}
