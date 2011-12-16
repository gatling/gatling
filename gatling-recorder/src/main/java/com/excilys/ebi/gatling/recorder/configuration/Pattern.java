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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
		result = prime * result
				+ ((patternType == null) ? 0 : patternType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Pattern))
			return false;
		Pattern other = (Pattern) obj;
		if (pattern == null) {
			if (other.pattern != null)
				return false;
		} else if (!pattern.equals(other.pattern))
			return false;
		if (patternType != other.patternType)
			return false;
		return true;
	}
	
	
}
