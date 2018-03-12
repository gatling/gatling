/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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
// Copyright 2013-2014 Richard M. Hightower
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//  		http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// __________                              _____          __   .__
// \______   \ ____   ____   ____   /\    /     \ _____  |  | _|__| ____    ____
//  |    |  _//  _ \ /  _ \ /    \  \/   /  \ /  \\__  \ |  |/ /  |/    \  / ___\
//  |    |   (  <_> |  <_> )   |  \ /\  /    Y    \/ __ \|    <|  |   |  \/ /_/  >
//  |______  /\____/ \____/|___|  / \/  \____|__  (____  /__|_ \__|___|  /\___  /
//         \/                   \/              \/     \/     \/       \//_____/
//      ____.                     ___________   _____    ______________.___.
//     |    |____ ___  _______    \_   _____/  /  _  \  /   _____/\__  |   |
//     |    \__  \\  \/ /\__  \    |    __)_  /  /_\  \ \_____  \  /   |   |
// /\__|    |/ __ \\   /  / __ \_  |        \/    |    \/        \ \____   |
// \________(____  /\_/  (____  / /_______  /\____|__  /_______  / / ______|
//               \/           \/          \/         \/        \/  \/
//

package io.advantageous.boon.core;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Supplier;

/**
 * This map only builds once you ask for a key for the first time.
 * It is designed to not incur the overhead of creating a map unless needed.
 *
 * Taken from Boon project (https://github.com/boonproject/boon)
 */
public class SimpleLazyMap extends AbstractMap {

  private Map map;
  private int size;
  private Object[] keys;
  private Object[] values;

  public SimpleLazyMap() {
    keys = new Object[5];
    values = new Object[5];
  }

  @Override
  public Object put(final Object key, final Object value) {
    if (map == null) {
      keys[size] = key;
      values[size] = value;
      size++;
      if (size == keys.length) {
        keys = grow(keys);
        values = grow(values);
      }
      return null;
    }
    return map.put(key, value);
  }

  @Override
  public Set keySet() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection values() {
    buildIfNeeded();
    return map.values();
  }

  @Override
  public Set<Map.Entry<Object, Object>> entrySet() {
    buildIfNeeded();
    return map.entrySet();
  }

  @Override
  public int size() {
    return map == null ? size : map.size();
  }

  @Override
  public boolean isEmpty() {
    return map == null ? size == 0 : map.isEmpty();
  }

  @Override
  public boolean containsKey(final Object key) {
    buildIfNeeded();
    return map.containsKey(key);
  }

  @Override
  public boolean containsValue(final Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object get(final Object key) {
    buildIfNeeded();
    return map.get(key);
  }

  private void buildIfNeeded() {
    if (map == null) {
      map = new LinkedHashMap<>();

      for (int index = 0; index < size; index++) {
        Object value = values[index];

        if (value instanceof Supplier) {
          value = ((Supplier) value).get();
        }

        map.put(keys[index], value);
      }
      this.keys = null;
      this.values = null;
    }
  }

  @Override
  public Object remove(final Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void putAll(final Map m) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  // ---------------------------------------------------------------- utils

  private static <V> V[] grow(final V[] array) {
    final Object newArray = Array.newInstance(array.getClass().getComponentType(), array.length * 2);
    System.arraycopy(array, 0, newArray, 0, array.length);
    return (V[]) newArray;
  }
}
