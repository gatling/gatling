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

package io.gatling.core.util;

import java.nio.ByteBuffer;

public class Longs {

  private final static long[] SIZE_TABLE = {
          9L,
          99L,
          999L,
          9999L,
          99999L,
          999999L,
          9999999L,
          99999999L,
          999999999L,
          9999999999L,
          99999999999L,
          999999999999L,
          9999999999999L,
          99999999999999L,
          999999999999999L,
          9999999999999999L,
          99999999999999999L,
          999999999999999999L,
          Long.MAX_VALUE };

  public static int positiveLongStringSize(long x) {
    for (int i = 0;; i++) {
      if (x <= SIZE_TABLE[i]) {
        return i + 1;
      }
    }
  }

  public static void writePositiveLongString(int i, ByteBuffer bb) {
    writePositiveLongString(i, positiveLongStringSize(i), bb);
  }

  public static void writePositiveLongString(long i, int stringSize, ByteBuffer bb) {
    long q;
    int r;
    int finalPosition = bb.position() + stringSize;
    int charPos = finalPosition;

    // Get 2 digits/iteration using longs until quotient fits into an int
    while (i > Integer.MAX_VALUE) {
      q = i / 100;
      // really: r = i - (q * 100);
      r = (int) (i - ((q << 6) + (q << 5) + (q << 2)));
      i = q;
      bb.put(--charPos, Integers.DIGIT_ONES[r]);
      bb.put(--charPos, Integers.DIGIT_TENS[r]);
    }

    // Get 2 digits/iteration using ints
    int q2;
    int i2 = (int) i;
    while (i2 >= 65536) {
      q2 = i2 / 100;
      // really: r = i2 - (q * 100);
      r = i2 - ((q2 << 6) + (q2 << 5) + (q2 << 2));
      i2 = q2;
      bb.put(--charPos, Integers.DIGIT_ONES[r]);
      bb.put(--charPos, Integers.DIGIT_TENS[r]);
    }

    // Fall thru to fast mode for smaller numbers
    // assert(i2 <= 65536, i2);
    for (;;) {
      q2 = (i2 * 52429) >>> (16 + 3);
      r = i2 - ((q2 << 3) + (q2 << 1)); // r = i2-(q2*10) ...
      bb.put(--charPos, Integers.DIGITS[r]);
      i2 = q2;
      if (i2 == 0)
        break;
    }

    bb.position(finalPosition);
  }
}
