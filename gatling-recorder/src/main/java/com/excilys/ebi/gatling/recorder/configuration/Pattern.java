package com.excilys.ebi.gatling.recorder.configuration;

import com.excilys.ebi.gatling.recorder.ui.enumeration.PatternType;

public class Pattern {
	
	private final PatternType patternType;
	private final String pattern;
	
	public Pattern(PatternType patternType, String pattern) {
		this.patternType = patternType;
		this.pattern = pattern;
	}

	public PatternType getPatternType() {
		return patternType;
	}

	public String getPattern() {
		return pattern;
	}
	
	@Override
	public String toString() {
		return patternType+" | "+pattern;
	}
}
