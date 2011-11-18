package com.excilys.ebi.gatling.recorder.ui.enumeration;

import java.text.Format;
import java.util.Date;

import org.apache.commons.lang.time.FastDateFormat;

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
		return FORMAT.format(date) + "_scenario." + extension;
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
