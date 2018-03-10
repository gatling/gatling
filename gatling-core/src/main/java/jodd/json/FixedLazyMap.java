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

//  Copyright 2015 Richard Hightower
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

package jodd.json;

import jodd.util.collection.MapEntry;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * This map only builds once you ask for a key for the first time.
 * It is designed to not incur the overhead of creating a map unless needed.
 *
 * Taken from Boon project (https://github.com/boonproject/boon)
 */
public class FixedLazyMap extends AbstractMap {

  private final boolean delayMap;
  private Map map;
  private int size;
  private Object[] keys;
  private Object[] values;

  public FixedLazyMap() {
    keys = new Object[5];
    values = new Object[5];
    this.delayMap = false;
  }

  public FixedLazyMap(final int initialSize) {
    keys = new Object[initialSize];
    values = new Object[initialSize];
    this.delayMap = false;
  }

  public FixedLazyMap(final int initialSize, final boolean delayMap) {
    keys = new Object[initialSize];
    values = new Object[initialSize];
    this.delayMap = delayMap;
  }

  public FixedLazyMap(final List keys, final List values, final boolean delayMap) {
    this.keys = array(Object.class, keys);
    this.values = array(Object.class, values);
    this.size = this.keys.length;
    this.delayMap = delayMap;

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

    if (delayMap) {

      return new FakeMapEntrySet(size, keys, values);
    }
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

  public LazyMap clearAndCopy() {
    final LazyMap map = new LazyMap(size);
    for (int index = 0; index < size; index++) {
      map.put(keys[index], values[index]);
    }
    size = 0;
    return map;
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

  private static <V> V[] array(final Class<V> cls, final Collection<V> collection) {
    final Object newInstance = Array.newInstance(cls, collection.size());
    return collection.toArray((V[]) newInstance);
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

  private static class FakeMapEntrySet extends AbstractSet<Entry<Object, Object>> {
    Map.Entry<Object, Object>[] array;

    public FakeMapEntrySet(final int size, final Object[] keys, final Object[] values) {
      array = new Map.Entry[size];

      for (int index = 0; index < size; index++) {
        array[index] = new MapEntry(keys[index], values[index]);
      }
    }

    @Override
    public Iterator<Entry<Object, Object>> iterator() {
      return list(this.array).iterator();
    }

    @Override
    public int size() {
      return array.length;
    }

  }

}