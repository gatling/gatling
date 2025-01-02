/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

// Forked from
// https://github.com/szeiger/perfio/blob/master/src/main/java/perfio/StringInternals.java
// published by Stefan Zeiger under Apache 2 License
package io.gatling.core.stats.writer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// Accessors for private String methods for faster string encoding that can be used when running
// with `--add-opens=java.base/java.lang=ALL-UNNAMED`.
// Lookup is implemented via load-time initialization of constant MethodHandles for optimal
// performance.
public final class StringInternals {
  private StringInternals() {}

  private static final MethodHandle coderMH, valueMH, newStringMH;
  private static final Throwable unavailabilityCause;

  static {
    Throwable unavailabilityCauseLocal = null;
    MethodHandle coderMHLocal = null;
    MethodHandle valueMHLocal = null;
    MethodHandle newStringMHLocal = null;
    try {
      MethodHandles.Lookup lookup =
          MethodHandles.privateLookupIn(String.class, MethodHandles.lookup());
      coderMHLocal = lookup.findVirtual(String.class, "coder", MethodType.methodType(byte.class));
      valueMHLocal = lookup.findVirtual(String.class, "value", MethodType.methodType(byte[].class));
      newStringMHLocal =
          lookup.findConstructor(
              String.class, MethodType.methodType(void.class, byte[].class, byte.class));

    } catch (Throwable t) {
      unavailabilityCauseLocal = t;
    }

    coderMH = coderMHLocal;
    valueMH = valueMHLocal;
    newStringMH = newStringMHLocal;
    unavailabilityCause = unavailabilityCauseLocal;
  }

  public static void checkAvailability() throws Throwable {
    if (unavailabilityCause != null) {
      throw unavailabilityCause;
    }
  }

  public static byte coder(String s) {
    try {
      return (byte) coderMH.invokeExact(s);
    } catch (Throwable t) {
      throw wrap(t);
    }
  }

  public static byte[] value(String s) {
    try {
      return (byte[]) valueMH.invokeExact(s);
    } catch (Throwable t) {
      throw wrap(t);
    }
  }

  public static String newString(byte[] bytes, byte coder) {
    try {
      return (String) newStringMH.invokeExact(bytes, coder);
    } catch (Throwable t) {
      throw wrap(t);
    }
  }

  private static RuntimeException wrap(Throwable t) {
    return t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
  }
}
