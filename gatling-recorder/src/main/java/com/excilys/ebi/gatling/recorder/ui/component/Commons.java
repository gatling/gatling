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
package com.excilys.ebi.gatling.recorder.ui.component;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

public class Commons {
	private static final ImageIcon favSmall = new ImageIcon(Commons.class.getResource("img/fav_small.png"));
	private static final ImageIcon favBig = new ImageIcon(Commons.class.getResource("img/fav_big.png"));
	private static final ImageIcon pictoSmall = new ImageIcon(Commons.class.getResource("img/picto_small.png"));
	private static final ImageIcon pictoBig = new ImageIcon(Commons.class.getResource("img/picto_big.png"));
	private static final ImageIcon logoSmall = new ImageIcon(Commons.class.getResource("img/logo_small.png"));

	private static final List<Image> imageList = new ArrayList<Image>();

	static {
		imageList.add(favSmall.getImage());
		imageList.add(favBig.getImage());
		imageList.add(pictoSmall.getImage());
		imageList.add(pictoBig.getImage());
	}

	public static List<Image> getIconList() {
		return imageList;
	}

	public static ImageIcon getGatlingImage() {
		return logoSmall;
	}

}
