/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.gatling.http.client.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Utf8UrlEncoderTest {

    private static String encodeQueryElement(String input) {
        StringBuilder sb = new StringBuilder(input.length() + 6);
        Utf8UrlEncoder.encodeAndAppendQueryElement(sb, input);
        return sb.toString();
    }

    @Test
    void testEncodeQueryElement() {
        assertEquals("foobar", encodeQueryElement("foobar"));
        // application/x-www-form-urlencoded leaves *, -, . and _ as is
        assertEquals("*-._", encodeQueryElement("*-._"));
        // space should be encoded as +
        assertEquals("+", encodeQueryElement(" "));
        // other chars should be encoded
        assertEquals("%7E%26%2B", encodeQueryElement("~&+"));
    }

    @Test
    void testPercentEncodeQueryElement() {
        assertEquals("foobar", Utf8UrlEncoder.percentEncodeQueryElement("foobar"));
        assertEquals("%2A%26%2B~_", Utf8UrlEncoder.percentEncodeQueryElement("*&+~_"));
    }
}
