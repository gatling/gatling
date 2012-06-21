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
package com.excilys.ebi.gatling.http.cookie;

import com.ning.http.client.Cookie;

public class HttpCookie extends Cookie {

	private final long whenCreated = System.currentTimeMillis();

	public HttpCookie(final Cookie c) {
		super(c.getDomain(), c.getName(), c.getValue(), c.getPath(), c
				.getMaxAge(), c.isSecure(), c.getVersion());
	}

	private static boolean equalsIgnoreCase(String s, String t) {
		if (s == t)
			return true;
		if ((s != null) && (t != null)) {
			return s.equalsIgnoreCase(t);
		}
		return false;
	}

	private static boolean equals(String s, String t) {
		if (s == t)
			return true;
		if ((s != null) && (t != null)) {
			return s.equals(t);
		}
		return false;
	}

	/**
	 * Test the equality of two HTTP cookies.
	 * 
	 * <p>
	 * The result is {@code true} only if two cookies come from same domain
	 * (case-insensitive), have same name (case-insensitive), and have same path
	 * (case-sensitive).
	 * 
	 * @return {@code true} if two HTTP cookies equal to each other; otherwise,
	 *         {@code false}
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof HttpCookie))
			return false;
		HttpCookie other = (HttpCookie) obj;

		// One http cookie equals to another cookie (RFC 2965 sec. 3.3.3) if:
		// 1. they come from same domain (case-insensitive),
		// 2. have same name (case-insensitive),
		// 3. and have same path (case-sensitive).
		return equalsIgnoreCase(getName(), other.getName())
				&& equalsIgnoreCase(getDomain(), other.getDomain())
				&& equals(getPath(), other.getPath());
	}

	/**
	 * Returns the hash code of this HTTP cookie. The result is the sum of hash
	 * code value of three significant components of this cookie: name, domain,
	 * and path. That is, the hash code is the value of the expression:
	 * <blockquote> getName().toLowerCase().hashCode()<br>
	 * + getDomain().toLowerCase().hashCode()<br>
	 * + getPath().hashCode() </blockquote>
	 * 
	 * @return this HTTP cookie's hash code
	 */
	@Override
	public int hashCode() {
		int h1 = getName().toLowerCase().hashCode();
		int h2 = (getDomain() != null) ? getDomain().toLowerCase().hashCode()
				: 0;
		int h3 = (getPath() != null) ? getPath().hashCode() : 0;

		return h1 + h2 + h3;
	}

	// Since the positive and zero max-age have their meanings,
	// this value serves as a hint as 'not specify max-age'
	private final static long MAX_AGE_UNSPECIFIED = -1;

	/**
	 * Reports whether this HTTP cookie has expired or not.
	 * 
	 * @return {@code true} to indicate this HTTP cookie has expired; otherwise,
	 *         {@code false}
	 */
	public boolean hasExpired() {
		if (getMaxAge() == 0)
			return true;

		// if not specify max-age, this cookie should be
		// discarded when user agent is to be closed, but
		// it is not expired.
		if (getMaxAge() == MAX_AGE_UNSPECIFIED)
			return false;

		long deltaSecond = (System.currentTimeMillis() - whenCreated) / 1000;
		return deltaSecond > getMaxAge();
	}

}
