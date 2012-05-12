/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.awt.Image
import java.util.ArrayList
import java.util.List

import javax.swing.ImageIcon

class Commons

object Commons {
	private val favSmall = new ImageIcon(classOf[Commons].getResource("img/fav_small.png"))
	private val favBig = new ImageIcon(classOf[Commons].getResource("img/fav_big.png"))
	private val pictoSmall = new ImageIcon(classOf[Commons].getResource("img/picto_small.png"))
	private val pictoBig = new ImageIcon(classOf[Commons].getResource("img/picto_big.png"))
	val logoSmall = new ImageIcon(classOf[Commons].getResource("img/logo_small.png"))

	private val imageList = new ArrayList[Image]

	val GATLING_RECORDER_FILE_NAME = "gatling-recorder.ini"
	
	imageList.add(favSmall.getImage)
	imageList.add(favBig.getImage)
	imageList.add(pictoSmall.getImage)
	imageList.add(pictoBig.getImage)

	val getIconList: List[Image] = imageList

}
