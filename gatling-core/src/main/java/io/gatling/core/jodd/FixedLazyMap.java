/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

package io.gatling.core.jodd;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * This map only builds once you ask for a key for the first time.
 * It is designed to not incur the overhead of creating a map unless needed.
 *
 * Taken from Boon project (https://github.com/boonproject/boon)
 */
public class FixedLazyMap extends AbstractMap {

  private LinkedHashMap map;
  private int size;
  private Object[] keys;
  private Object[] values;

  public FixedLazyMap() {
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
  public boolean containsValue(final Object value) {
    // don't load map if they are just a few entries
    if (map == null && size < 5) {
      for (int index = 0; index < size; index++) {
        if (values[index].equals(value)) {
          return true;
        }
      }
      return false;
    }
    buildIfNeeded();
    return map.containsValue(value);
  }

  @Override
  public boolean containsKey(final Object key) {
    // don't load map if they are just a few entries
    if (map == null && size < 5) {
      for (int index = 0; index < size; index++) {
        if (keys[index].equals(key)) {
          return true;
        }
      }
      return false;
    }
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
    return map == null ? set(size, keys) : map.keySet();

  }

  @Override
  public Collection values() {
    return map == null ? Arrays.asList(values) : map.values();

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
    return map == null ? null : map.clone();
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

  private static <V> List<V> list(final V... array) {
    final int length = array.length;
    final List<V> list = new ArrayList<>();
    for (int index = 0; index < length; index++) {
      list.add((V) Array.get(array, index));
    }
    return list;
  }
}
