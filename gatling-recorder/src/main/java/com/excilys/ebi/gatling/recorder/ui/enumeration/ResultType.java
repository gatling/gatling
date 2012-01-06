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
package com.excilys.ebi.gatling.recorder.ui.enumeration;

import java.text.Format;
import java.util.Date;

import org.apache.commons.lang.time.FastDateFormat;

import com.excilys.ebi.gatling.recorder.configuration.Configuration;

public enum ResultType {

	TEXT("txt", "scenarioText.vm", "Text"),

	SCALA("scala", "scenarioScala.vm", "Scala");

	public static final Format FORMAT = FastDateFormat.getInstance("yyyyMMddHHmmss");

	private final String extension;

	private final String template;

	private final String label;

	ResultType(String extension, String template, String label) {
		this.extension = extension;
		this.template = template;
		this.label = label;
	}

	public String getScenarioFileName(Date date) {
		if (Configuration.getInstance().getIdePackage() != null)
			return "Simulation" + FORMAT.format(date) + "." + extension;
		else
			return FORMAT.format(date) + "_scenario@default_" + extension + "." + extension;
	}

	public String getTemplate() {
		return template;
	}

	public String getLabel() {
		return label;
	}

	public static ResultType getByLabel(String label) {
		for (ResultType value : ResultType.values()) {
			if (value.label.equals(label)) {
				return value;
			}
		}
		return null;
	}
}
