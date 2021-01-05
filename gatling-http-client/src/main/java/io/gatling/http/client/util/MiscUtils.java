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

//
// Copyright (c) 2018 AsyncHttpClient Project. All rights reserved.
//
// This program is licensed to you under the Apache License Version 2.0,
// and you may not use this file except in compliance with the Apache License Version 2.0.
// You may obtain a copy of the Apache License Version 2.0 at
//     http://www.apache.org/licenses/LICENSE-2.0.
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the Apache License Version 2.0 is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
//

package io.gatling.http.client.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class MiscUtils {

  private MiscUtils() {
  }

  public static boolean isNonEmpty(String string) {
    return !isEmpty(string);
  }

  public static boolean isEmpty(String string) {
    return string == null || string.isEmpty();
  }

  public static boolean isNonEmpty(Object[] array) {
    return array != null && array.length != 0;
  }

  public static boolean isNonEmpty(byte[] array) {
    return array != null && array.length != 0;
  }

  public static boolean isNonEmpty(Collection<?> collection) {
    return collection != null && !collection.isEmpty();
  }

  public static boolean isNonEmpty(Map<?, ?> map) {
    return map != null && !map.isEmpty();
  }

  public static <T> T withDefault(T value, T def) {
    return value == null ? def : value;
  }

  public static void closeSilently(Closeable closeable) {
    if (closeable != null)
      try {
        closeable.close();
      } catch (IOException e) {
        //
      }
  }
}
