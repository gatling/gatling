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

package io.gatling.core.javaapi;

import scala.Option;
import scala.collection.Seq;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.gatling.core.javaapi.internal.ScalaHelpers.*;

public class Session {

  private final io.gatling.core.session.Session wrapped;

  public Session(io.gatling.core.session.Session wrapped) {
    this.wrapped = wrapped;
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    Option<Object> valueOption = wrapped.attributes().get(key);
    return valueOption.isDefined() ? (T) valueOption.get() : null;
  }

  public String getString(String key) {
    Option<Object> valueOption = wrapped.attributes().get(key);
    return valueOption.isDefined() ? valueOption.get().toString() : null;
  }

  public Integer getIntegerWrapper(String key) {
    Option<Object> valueOption = wrapped.attributes().get(key);
    if (valueOption.isDefined()) {
      Object value = valueOption.get();
      if (value instanceof Integer) {
        return (Integer) value;
      } else if (value instanceof String) {
        return Integer.valueOf((String) value);
      } else {
        throw new ClassCastException(value + " is not an Integer: " + value.getClass());
      }
    } else {
      return null;
    }
  }

  public int getInt(String key) {
    Object value = wrapped.attributes().apply(key);
    if (value instanceof Integer) {
      return ((Integer) value).intValue();
    } else if (value instanceof String) {
      return Integer.parseInt((String) value);
    } else {
      throw new ClassCastException(value + " is not an Integer: " + value.getClass());
    }
  }

  public Long getLongWrapper(String key) {
    Option<Object> valueOption = wrapped.attributes().get(key);
    if (valueOption.isDefined()) {
      Object value = valueOption.get();
      if (value instanceof Integer) {
        return ((Integer) value).longValue();
      } else if (value instanceof Long) {
        return (Long) value;
      } else if (value instanceof String) {
        return Long.valueOf((String) value);
      } else {
        throw new ClassCastException(value + " is not an Long: " + value.getClass());
      }
    } else {
      return null;
    }
  }

  public long getLong(String key) {
    Object value = wrapped.attributes().apply(key);
    if (value instanceof Integer) {
      return ((Integer) value).longValue();
    } else if (value instanceof Long) {
      return ((Long) value).longValue();
    } else if (value instanceof String) {
      return Long.parseLong((String) value);
    } else {
      throw new ClassCastException(value + " is not an Long: " + value.getClass());
    }
  }

  public Double getDoubleWrapper(String key) {
    Option<Object> valueOption = wrapped.attributes().get(key);
    if (valueOption.isDefined()) {
      Object value = valueOption.get();
      if (value instanceof Integer) {
        return ((Integer) value).doubleValue();
      } else if (value instanceof Long) {
        return ((Long) value).doubleValue();
      } else if (value instanceof Double) {
        return (Double) value;
      } else if (value instanceof String) {
        return Double.valueOf((String) value);
      } else {
        throw new ClassCastException(value + " is not an Double: " + value.getClass());
      }
    } else {
      return null;
    }
  }

  public double getDouble(String key) {
    Object value = wrapped.attributes().apply(key);
    if (value instanceof Integer) {
      return ((Integer) value).doubleValue();
    } else if (value instanceof Long) {
      return ((Long) value).doubleValue();
    } else if (value instanceof Double) {
      return ((Double) value).doubleValue();
    } else if (value instanceof String) {
      return Long.parseLong((String) value);
    } else {
      throw new ClassCastException(value + " is not an Double: " + value.getClass());
    }
  }


  public Boolean getBooleanWrapper(String key) {
    Option<Object> valueOption = wrapped.attributes().get(key);
    if (valueOption.isDefined()) {
      Object value = valueOption.get();
      if (value instanceof Boolean) {
        return (Boolean) value;
      } else if (value instanceof String) {
        return Boolean.valueOf((String) value);
      } else {
        throw new ClassCastException(value + " is not an Boolean: " + value.getClass());
      }
    } else {
      return null;
    }
  }

  public boolean getBoolean(String key) {
    Object value = wrapped.attributes().apply(key);
    if (value instanceof Boolean) {
      return ((Boolean) value).booleanValue();
    } else if (value instanceof String) {
      return Boolean.parseBoolean((String) value);
    } else {
      throw new ClassCastException(value + " is not an Boolean: " + value.getClass());
    }
  }

  public List<Object> getList(String key) {
    Option<Object> valueOption = wrapped.attributes().get(key);
    if (valueOption.isDefined()) {
      Object value = valueOption.get();
      if (value instanceof List<?>) {
        return ((List<Object>) value);
      } else if (value instanceof Seq<?>) {
        return toJavaList((Seq<Object>) value);
      } else {
        throw new ClassCastException(value + " is not an List: " + value.getClass());
      }
    } else {
      return null;
    }
  }

  public Set<Object> getSet(String key) {
    Option<Object> valueOption = wrapped.attributes().get(key);
    if (valueOption.isDefined()) {
      Object value = valueOption.get();
      if (value instanceof Set<?>) {
        return ((Set<Object>) value);
      } else if (value instanceof scala.collection.Set<?>) {
        return toJavaSet((scala.collection.Set<?>) value);
      } else {
        throw new ClassCastException(value + " is not an Set: " + value.getClass());
      }
    } else {
      return null;
    }
  }

  public Map<String, Object> getMap(String key) {
    Option<Object> valueOption = wrapped.attributes().get(key);
    if (valueOption.isDefined()) {
      Object value = valueOption.get();
      if (value instanceof Map<?, ?>) {
        return ((Map<String, Object>) value);
      } else if (value instanceof scala.collection.Map<?, ?>) {
        return toJavaMap((scala.collection.Map<String, Object>) value);
      } else {
        throw new ClassCastException(value + " is not an Map: " + value.getClass());
      }
    } else {
      return null;
    }
  }

  public Session set(String key, Object value) {
    return new Session(wrapped.set(key, value));
  }

  public Session setAll(Map<String, Object> newAttributes) {
    return new Session(wrapped.setAll(toScalaMap(newAttributes)));
  }

  public Session remove(String key) {
    return new Session(wrapped.remove(key));
  }

  public Session removeAll(String... keys) {
    return new Session(wrapped.removeAll(toScalaSeq(keys)));
  }

  public boolean contains(String key) {
    return wrapped.contains(key);
  }

  public io.gatling.core.session.Session asScala() {
    return wrapped;
  }
}
