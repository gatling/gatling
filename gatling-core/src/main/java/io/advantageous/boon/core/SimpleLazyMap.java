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
import java.util.concurrent.ConcurrentHashMap;
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
  public Set<Entry<Object, Object>> entrySet() {
    if (map != null) map.entrySet();
    buildIfNeeded();
    return map.entrySet();
  }

  @Override
  public int size() {
    if (map == null) {
      return size;
    }
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    if (map == null) {
      return size == 0;
    }
    return map.isEmpty();
  }

  @Override
  public boolean containsValue(final Object value) {
    if (map == null) {
      throw new RuntimeException("wrong type of map");
    }
    return map.containsValue(value);
  }

  @Override
  public boolean containsKey(final Object key) {
    buildIfNeeded();
    return map.containsKey(key);
  }

  @Override
  public Object get(final Object key) {
    buildIfNeeded();
    return map.get(key);
  }

  private void buildIfNeeded() {
    if (map == null) {
      map = new LinkedHashMap<>(size, 0.01f);

      for (int index = 0; index < size; index++) {
        Object value = values[index];

        if (value instanceof Supplier) {
          value = ((Supplier)value).get();
        }

        map.put(keys[index], value);
      }
      this.keys = null;
      this.values = null;
    }
  }

  @Override
  public Object remove(final Object key) {

    buildIfNeeded();
    return map.remove(key);

  }

  @Override
  public void putAll(final Map m) {
    buildIfNeeded();
    map.putAll(m);
  }

  @Override
  public void clear() {
    if (map == null) {
      size = 0;
    } else {
      map.clear();
    }
  }

  @Override
  public Set keySet() {
    if (map == null) {
      return set(size, keys);
    }
    return map.keySet();

  }

  @Override
  public Collection values() {
    if (map == null) {
      return Arrays.asList(values);
    }
    return map.values();

  }

  @Override
  public boolean equals(final Object o) {
    buildIfNeeded();
    return map.equals(o);
  }

  @Override
  public int hashCode() {
    buildIfNeeded();
    return map.hashCode();
  }

  @Override
  public String toString() {
    buildIfNeeded();
    return map.toString();
  }

  @Override
  protected Object clone() {
    if (map == null) {
      return null;
    }
    if (map instanceof LinkedHashMap) {
      return ((LinkedHashMap) map).clone();
    }
    return copy(this);
  }

  public static <K, V> Map<K, V> copy(final Map<K, V> map) {
    if (map instanceof LinkedHashMap) {
      return new LinkedHashMap<>(map);
    }
    if (map instanceof ConcurrentHashMap) {
      return new ConcurrentHashMap<>(map);
    }
    return new HashMap<>(map);
  }


  // ---------------------------------------------------------------- utils

  private static <V> Set<V> set(final int size, final V... array) {
    int index = 0;
    final Set<V> set = new HashSet<>();

    for (final V v : array) {
      set.add(v);
      index++;
      if (index == size) {
        break;
      }
    }
    return set;
  }

  private static <V> V[] grow(final V[] array) {
    final Object newArray = Array.newInstance(array.getClass().getComponentType(), array.length * 2);
    System.arraycopy(array, 0, newArray, 0, array.length);
    return (V[]) newArray;
  }
}
