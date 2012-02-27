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
package com.excilys.ebi.gatling.core.util;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class HtmlHelper {

	private HtmlHelper() {
		throw new UnsupportedOperationException();
	}

	private static final ResourceBundle ENTITIES = ResourceBundle.getBundle("html-entities");

	private static final Map<Integer, String> CHAR_TO_HTML_ENTITIES = new HashMap<Integer, String>();

	private static final Map<String, Integer> HTML_ENTITIES_TO_CHAR = new HashMap<String, Integer>();

	static {
		for (String entityName : ENTITIES.keySet()) {
			Integer charCode = Integer.valueOf(ENTITIES.getString(entityName));
			HTML_ENTITIES_TO_CHAR.put(entityName, charCode);
			CHAR_TO_HTML_ENTITIES.put(charCode, entityName);
		}
	}

	public static Integer htmlEntityToChar(String entity) {
		return HTML_ENTITIES_TO_CHAR.get(entity);
	}

	public static String charToHtmlEntity(char entity) {
		return CHAR_TO_HTML_ENTITIES.get(entity);
	}

	public static String htmlEscape(String string) {

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < string.length(); i++) {
			char nonEscaped = string.charAt(i);
			String escaped = charToHtmlEntity(nonEscaped);
			if (escaped == null)
				builder.append(nonEscaped);
			else
				builder.append(escaped);
		}

		return builder.toString();
	}
}
