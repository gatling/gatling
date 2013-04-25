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
package com.excilys.ebi.gatling.recorder.ui;

import javax.swing.ImageIcon

class Commons

object Commons {
	private val favSmall = new ImageIcon(getClass.getResource("img/fav_small.png"))
	private val favBig = new ImageIcon(getClass.getResource("img/fav_big.png"))
	private val pictoSmall = new ImageIcon(getClass.getResource("img/picto_small.png"))
	private val pictoBig = new ImageIcon(getClass.getResource("img/picto_big.png"))
	val logoSmall = new ImageIcon(getClass.getResource("img/logo_small.png"))

	val GATLING_RECORDER_FILE_NAME = "gatling-recorder.ini"

	val iconList = List(favSmall.getImage, favBig.getImage, pictoSmall.getImage, pictoBig.getImage)
}
